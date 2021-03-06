/* IBM_PROLOG_BEGIN_TAG                                                   */
/* This is an automatically generated prolog.                             */
/*                                                                        */
/* $Source: src/kv/arp.c $                                                */
/*                                                                        */
/* IBM Data Engine for NoSQL - Power Systems Edition User Library Project */
/*                                                                        */
/* Contributors Listed Below - COPYRIGHT 2014,2015                        */
/* [+] International Business Machines Corp.                              */
/*                                                                        */
/*                                                                        */
/* Licensed under the Apache License, Version 2.0 (the "License");        */
/* you may not use this file except in compliance with the License.       */
/* You may obtain a copy of the License at                                */
/*                                                                        */
/*     http://www.apache.org/licenses/LICENSE-2.0                         */
/*                                                                        */
/* Unless required by applicable law or agreed to in writing, software    */
/* distributed under the License is distributed on an "AS IS" BASIS,      */
/* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or        */
/* implied. See the License for the specific language governing           */
/* permissions and limitations under the License.                         */
/*                                                                        */
/* IBM_PROLOG_END_TAG                                                     */
/**
 *******************************************************************************
 * \file
 * \brief
 *   ark thread pool functions
 ******************************************************************************/
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

#include <sys/stat.h>
#include <sys/time.h>

#include "ct.h"
#include "ut.h"
#include "vi.h"
#include "arkdb.h"
#include "ark.h"
#include "arp.h"
#include "am.h"
#include <ticks.h>

#include <arkdb_trace.h>
#include <errno.h>

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
int ark_wait_tag(_ARK *_arkp, int tag, int *errcode, int64_t *res)
{
  int rc = 0;
  rcb_t *rcbp = &(_arkp->rcbs[tag]);

  KV_TRC_DBG(pAT, "TAGWAIT: tag:%d", tag);

  pthread_mutex_lock(&(rcbp->alock));
  while (rcbp->stat != A_COMPLETE)
  {
    pthread_cond_wait(&(rcbp->acond), &(rcbp->alock));
  }

  if (rcbp->stat == A_COMPLETE)
  {
      *res = rcbp->res;
      *errcode = rcbp->rc;
      rcbp->stat = A_NULL;
      tag_bury(_arkp->rtags, tag);
  }
  else
  {
    rc = EINVAL;
    KV_TRC_FFDC(pAT, "tag = %d rc = %d", tag, rc);
  }

  pthread_mutex_unlock(&(rcbp->alock));

  return rc;
}

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
int ark_anyreturn(_ARK *_arkp, int *tag, int64_t *res)
{
  int i;
  int astart = _arkp->astart;
  int nasyncs = _arkp->nasyncs;
  int itag = -1;

  for (i = 0; i < _arkp->nasyncs; i++)
  {
      itag = (astart + i) % nasyncs;
      if (_arkp->rcbs[itag].stat == A_COMPLETE)
      {
          *tag = itag;
          *res = _arkp->rcbs[(astart+i) % nasyncs].res;
          _arkp->rcbs[itag].stat = A_NULL;
          tag_bury(_arkp->rtags, itag);
          return 0;
      }
  }
  _arkp->astart++;
  _arkp->astart %= nasyncs;
  return EINVAL;
}

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
int64_t ark_take_pool(_ARK *_arkp, ark_stats_t *stats, uint64_t n)
{
  int64_t  rc  = 0;
  uint64_t blk = 0;

  pthread_mutex_lock(&_arkp->mainmutex);

  bl_check_take(_arkp->bl, n);

  if (bl_left(_arkp->bl) < n)
  {
      BL      *bl      = NULL;
      int64_t  cursize = _arkp->size;
      int64_t  atleast = n * _arkp->bsize;
      int64_t  newsize = cursize + atleast + (_arkp->grow * _arkp->bsize);
      uint64_t newbcnt = newsize / _arkp->bsize;

      rc = ea_resize(_arkp->ea, _arkp->bsize, newbcnt);
      if (rc)
      {
          rc = -1;
          goto error;
      }

      bl = bl_resize(_arkp->bl, newbcnt, _arkp->bl->w);
      if (bl == NULL)
      {
          rc = -1;
          goto error;
      }

      _arkp->size = newsize;
      _arkp->bl   = bl;
  }

  blk = bl_take(_arkp->bl, n);
  if (blk <= 0)
  {
      rc = -1;
      goto error;
  }

  _arkp->blkused += n;
  stats->blk_cnt += n;
  rc              = blk;

error:
  pthread_mutex_unlock(&_arkp->mainmutex);

  if (rc <= 0) KV_TRC_FFDC(pAT, "ERROR");
  return rc;
}

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
void ark_drop_pool(_ARK *_arkp, ark_stats_t *stats, uint64_t blk)
{
  int nblks = 0;

  pthread_mutex_lock(&_arkp->mainmutex);
  nblks = bl_drop(_arkp->bl, blk);
  if (nblks == -1)
  {
      KV_TRC_FFDC(pAT, "failure, unable to decrement blocks for blk:%ld", blk);
  }
  else
  {
      _arkp->blkused -= nblks;
      stats->blk_cnt -= nblks;
  }
  pthread_mutex_unlock(&_arkp->mainmutex);
  KV_TRC_DBG(pAT, "blkused %ld new %d", _arkp->blkused, nblks);
}

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
int ark_enq_cmd(int cmd, _ARK *_arkp, uint64_t klen, void *key,
                uint64_t vlen,void *val,uint64_t voff,
                void (*cb)(int errcode, uint64_t dt,int64_t res),
                uint64_t dt, int pthr, int *ptag)
{
  int32_t   rtag = -1;
  int       rc = 0;
  int       pt = 0;
  rcb_t    *rcbp = NULL;

  if ( (_arkp == NULL) || ((klen > 0) && (key == NULL)))
  {
    KV_TRC_FFDC(pAT, "rc = EINVAL: cmd %d, ark %p, \
                     key %p, klen %"PRIu64"", cmd, _arkp, key, klen);
    rc = EINVAL;
    goto ark_enq_cmd_err;
  }

  rc = tag_unbury(_arkp->rtags, &rtag);
  if (rc == 0)
  {
    rcbp        = &(_arkp->rcbs[rtag]);
    rcbp->stime = getticks();
    rcbp->ark   = _arkp;
    rcbp->cmd   = cmd;
    rcbp->rtag  = rtag;
    rcbp->ttag  = -1;
    rcbp->stat  = A_INIT;
    rcbp->klen  = klen;
    rcbp->key   = key;
    rcbp->vlen  = vlen;
    rcbp->val   = val;
    rcbp->voff  = voff;
    rcbp->cb    = cb;
    rcbp->dt    = dt;
    rcbp->rc    = 0;
    rcbp->hold  = -1;
    rcbp->res   = -1;

    // If the caller has asked to run on a specific thread, then
    // do so.  Otherwise, use the key to find which thread
    // will handle command
    if (pthr == -1)
    {
      rcbp->pos   = hash_pos(_arkp->ht, key, klen);
      rcbp->sthrd = rcbp->pos / _arkp->npart;
      pt          = rcbp->pos / _arkp->npart;
    }
    else
    {
      rcbp->sthrd = pthr;
      pt = pthr;
    }

    KV_TRC_DBG(pAT, "IO:     RQ_ENQ NEW_REQ tid:%d rtag:%d cmd:%d",
               rcbp->sthrd, rtag, cmd);

    queue_lock(_arkp->poolthreads[pt].rqueue);

    (void)queue_enq_unsafe(_arkp->poolthreads[pt].rqueue, rtag);

    queue_unlock(_arkp->poolthreads[pt].rqueue);

  }
  else
  {
    KV_TRC_DBG(pAT, "NO_TAG: %"PRIx64"", dt);
  }

ark_enq_cmd_err:

  // If the caller is interested in the tag, give it back to
  // them.  If they aren't, no need to set it
  if (ptag != NULL)
  {
    *ptag = rtag;
  }
  return rc;
}

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
void ark_rand_pool(_ARK *_arkp, int id, tcb_t *tcbp)
{
  // find the hashtable position
  rcb_t   *rcbp = &(_arkp->rcbs[tcbp->rtag]);
  int32_t  found = 0;
  int32_t  i = 0;
  int32_t  ea_rc = 0;
  uint8_t  *buf = NULL;
  uint8_t  *pos = NULL;
  uint64_t hblk;
  uint64_t pkl;
  uint64_t pvl;
  uint64_t btsize;
  int64_t  blen = 0;
  void     *kbuf = rcbp->key;
  BT       *bt = NULL;
  BT       *bt_orig = NULL;
  ark_io_list_t *bl_array = NULL;
  scb_t    *scbp = &(_arkp->poolthreads[id]);

  // Check to see if this thead has any keys to begin with.
  // If it doesn't reset the rlast field and return immediately.
  if (scbp->poolstats.kv_cnt == 0)
  {
    rcbp->res = -1;
    scbp->rlast = -1;
    rcbp->rc = EAGAIN;
    goto ark_rand_pool_err;
  }

  // Check to see if we start off at the beginning
  // of the thread's monitored hash buckets or
  // if we need to pick up where we left off last time
  i = scbp->rlast;
  if ( i == -1 )
  {
    i = id;
  }

  // Now that we have the starting point, loop
  // through the buckets for this thread and find
  // the first bucket with blocks
  for (; (i < _arkp->hcount) && (!found); i += _arkp->nthrds)
  {

    hblk = HASH_LBA(HASH_GET(_arkp->ht, i));

    // Found an entry with blocks
    if (hblk>0)
    {
      blen = bl_len(_arkp->bl, hblk);
      bt = bt_new(divceil(blen * _arkp->bsize, 
                                 _arkp->bsize) * _arkp->bsize, 
                                 _arkp->vlimit, 8, &(btsize), &bt_orig);
      if (bt == NULL)
      {
        rcbp->res = -1;
        rcbp->rc = ENOMEM;
        KV_TRC_FFDC(pAT, "Bucket create failed blen %"PRIi64"", blen);
        goto ark_rand_pool_err;
      }

      buf = (uint8_t *)bt;
      bl_array = bl_chain(_arkp->bl, hblk, blen);
      if (bl_array == NULL)
      {
        rcbp->res = -1;
        rcbp->rc = ENOMEM;
        KV_TRC_FFDC(pAT, "Can not create block list %"PRIi64"", blen);
        bt_delete(bt_orig);
        goto ark_rand_pool_err;
      }

      ea_rc = ea_async_io(_arkp->ea, ARK_EA_READ, (void *)buf, 
                          bl_array, blen, _arkp->nthrds);
      if (ea_rc != 0)
      {
        rcbp->res = -1;
        rcbp->rc = ea_rc;
        free(bl_array);
        bt_delete(bt_orig);
        bt = NULL;
        KV_TRC_FFDC(pAT, "IO failure for READ rc = %d", ea_rc);
        goto ark_rand_pool_err;
      }

      free(bl_array);

      found = 1;
    }
  }

  if (found)
  {
    // Start position at the beginning of the bucket
    pos = bt->data;

    // If we've made it here, we have our bucket, whether it be 
    // a new bucket from a new hash table entry, or the old 
    // bucket that still has keys that haven't been processed.
    pos += vi_dec64(pos, &pkl);
    pos += vi_dec64(pos, &pvl);

    // Record the true length of the key
    rcbp->res = pkl;

    // Copy the key into the buffer for the passed in length
    memcpy(kbuf, pos, (pkl > rcbp->klen? rcbp->klen : pkl));

    // Delete the bucket since we are now done with it
    bt_delete(bt_orig);

    // This will be the starting point on the next call to ark_random
    // for this thread
    scbp->rlast = i + _arkp->nthrds;
  }
  else
  {
    rcbp->res = -1;

    // We didn't find a key here, so return EAGAIN so that
    // ark_random can determine if it wants to try on the
    // next pool thread.  Also reset the start point
    // for this thread
    scbp->rlast = -1;
    rcbp->rc = EAGAIN;
  }

ark_rand_pool_err:
  tcbp->state = ARK_CMD_DONE;
}

/**
 *******************************************************************************
 * \brief
 * \details
 *  arp->res returns the length of the key\n
 *  arp->val is for the ARI structure...both for passing in and returning.
 ******************************************************************************/
void ark_first_pool(_ARK *_arkp, int id, tcb_t *tcbp)
{
  rcb_t   *rcbp = &(_arkp->rcbs[tcbp->rtag]);
  _ARI *_arip = NULL;
  uint64_t i;
  int32_t  rc = 0;
  int32_t  found = 0;
  uint8_t  *buf = NULL;
  uint8_t  *pos = NULL;
  uint64_t hblk;
  uint64_t pkl;
  uint64_t pvl;
  int64_t  blen = 0;
  void     *kbuf = rcbp->key;
  ark_io_list_t *bl_array = NULL;

  _arip = (_ARI *)rcbp->val;
  _arip->ark = _arkp;
  _arip->bt = NULL;
  _arip->hpos = 0;
  _arip->key = 0;
  _arip->pos = NULL;

  // Now that we have the starting point, loop
  // through the buckets for this thread and find
  // the first bucket with blocks
  for (i = id; (i < _arkp->hcount) && (!found); i += _arkp->nthrds)
  {
    hblk = HASH_LBA(HASH_GET(_arkp->ht, i));

    // Found an entry with blocks
    if (hblk>0)
    {
      // Remember the hash table entry.  Will start from
      // this point.
      _arip->hpos = i;        

      // Allocate a new bucket
      blen = bl_len(_arkp->bl, hblk);
      _arip->bt = bt_new(divceil(blen * _arkp->bsize, 
                                 _arkp->bsize) * _arkp->bsize, 
                                 _arkp->vlimit, 8, &(_arip->btsize),
                                 &(_arip->bt_orig));
      if (_arip->bt == NULL)
      {
        rcbp->rc = ENOMEM;
        rcbp->res = -1;
        goto ark_first_pool_err;
      }

      buf = (uint8_t *)_arip->bt;

      bl_array = bl_chain(_arkp->bl, hblk, blen);
      if (bl_array == NULL)
      {
        rcbp->rc = ENOMEM;
        bt_delete(_arip->bt_orig);
        _arip->bt_orig = NULL;
        _arip->bt = NULL;
        rcbp->res = -1;
        goto ark_first_pool_err;
      }

      // Iterate over all the blocks and read them from
      // the storage into the bucket.
      rc = ea_async_io(_arkp->ea, ARK_EA_READ, (void *)buf, 
                       bl_array, blen, _arkp->nthrds);
      if (rc != 0)
      {
        KV_TRC_FFDC(pAT, "FFDC, errno = %d", errno);
        free(bl_array);
        bt_delete(_arip->bt_orig);
        _arip->bt_orig = NULL;
        _arip->bt = NULL;
        rcbp->rc = rc;
        rcbp->res = -1;
        goto ark_first_pool_err;
      }

      free(bl_array);

      found = 1;
    }
  }

  // If bt == NULL, that either means we didn't find a
  // hash bucket with a key or we ran into an error.
  if (_arip->bt == NULL) {
    if (rc == 0)
    {
      // If rc == 0, that means we didn't find a hash
      // table.  Set the error EAGAIN so the caller
      // can retry on a different thread.
      rcbp->rc = EAGAIN;
    }
    rcbp->res = -1;
    goto ark_first_pool_err;
  }

  // Look for the first key in the bucket
  pos = _arip->bt->data;
  pos += vi_dec64(pos, &pkl);
  pos += vi_dec64(pos, &pvl);

  // Record the true length of the key
  rcbp->res = pkl;

  // Copy the key into the buffer for the passed in length
  memcpy(kbuf, pos, (pkl > rcbp->klen) ? rcbp->klen : pkl);

  // Are we done with this bucket or do we remember which key we
  // left off on for next call.
  if (_arip->bt->cnt > 1)
  {
    _arip->key++;
  }
  else
  {
    // If there are no more keys in this hash bucket
    // then jump to the next monitored hash bucket for this thread.
    _arip->hpos += _arkp->nthrds;
    _arip->key = 0;
  }

ark_first_pool_err:

  if (_arip->bt != NULL)
  {
    bt_delete(_arip->bt_orig);
    _arip->bt_orig = NULL;
    _arip->bt = NULL;
  }

  tcbp->state = ARK_CMD_DONE;
}

/**
 *******************************************************************************
 * \brief
 * \details
 *  arp->res returns the length of the key\n
 *  arp->val is for the ARI structure...both for passing in and returning.
 ******************************************************************************/
void ark_next_pool(_ARK *_arkp, int id, tcb_t *tcbp)
{
  rcb_t    *rcbp = &(_arkp->rcbs[tcbp->rtag]);
  _ARI     *_arip = (_ARI *)rcbp->val;
  uint64_t i;
  int32_t  rc = 0;
  int32_t  found = 0;
  int32_t  kcnt = 0;
  uint8_t  *buf = NULL;
  uint8_t  *pos = NULL;
  uint64_t hblk;
  uint64_t pkl;
  uint64_t pvl;
  int64_t  blen = 0;
  void     *kbuf = rcbp->key;
  ark_io_list_t *bl_array = NULL;
  
  rcbp->res = -1;

  // We can be in a few different scenarios when we enter
  // here.
  //
  // 1. We are starting out with a new thread, so we
  //    start from the beginning of it's monitored
  //    hash buckets.
  //    _arip->key == 0;
  // 2. We are back with the same thread as before, but
  //    we are starting a new hash bucket.
  //    _arip->key == 0;
  // 3. We are back with the same thread as before, but
  //    we are in the middle of a hash bucket.
  //    _arip->key > 0;
  //
  // Previously, we would cache the hash bucket in the ARI
  // structure.  But, because the ark library now supports
  // multi-threading, the hash bucket can change inbetween
  // calls to ark_first and ark_next and ark_next.  So
  // now we must fetch the hash bucket on each invocation.
  //

  // Look for the first hash table entry that has blocks
  // in it.  If hpos is zero, we are just starting out
  // with a new pool thread, so start with that pool
  // thread's first hash bucket.
  i = _arip->hpos;
  if (_arip->hpos == 0)
  {
    i = id;
  }

  for (; (i < _arkp->hcount) && (!found); i += _arkp->nthrds) {
    hblk = HASH_LBA(HASH_GET(_arkp->ht, i));

    // Found an entry with blocks
    if (hblk>0)
    {
      // Remember the hash table entry.  Will start from
      // this point next time.
      _arip->hpos = i;        

      // Allocate a new bucket
      blen = bl_len(_arkp->bl, hblk);
      _arip->bt = bt_new(divceil(blen * _arkp->bsize, 
                                 _arkp->bsize) * _arkp->bsize, 
                           _arkp->vlimit, 8, &(_arip->btsize), 
                           &(_arip->bt_orig));
      if (_arip->bt == NULL)
      {
        rcbp->rc = ENOMEM;
        goto ark_next_pool_err;
      }

      buf = (uint8_t *)_arip->bt;
      bl_array = bl_chain(_arkp->bl, hblk, blen);
      if (bl_array == NULL)
      {
        rcbp->rc = ENOMEM;
        bt_delete(_arip->bt_orig);
        _arip->bt_orig = NULL;
        _arip->bt = NULL;
        goto ark_next_pool_err;
      }

      rc = ea_async_io(_arkp->ea, ARK_EA_READ, (void *)buf, 
                          bl_array, blen, _arkp->nthrds);
      if (rc != 0)
      {
        free(bl_array);
        KV_TRC_FFDC(pAT, "FFDC, ENOENT errno = %d", errno);
        bt_delete(_arip->bt_orig);
        _arip->bt_orig = NULL;
        _arip->bt = NULL;
        rcbp->rc = rc;
        goto ark_next_pool_err;
      }

      free(bl_array);

      // The hash bucket we just read in, does it have
      // enough keys that we can get the "_arip->key"
      // entry?  If not, then release this hash bucket
      // and look to the next one
      if (_arip->key < _arip->bt->cnt)
      {
        found = 1;
      }
      else
      {
        // This hash bucket has changed and no longer
        // contains the same amount of keys.  Move
        // to the next bucket and start with the first (0)
        // key.
        bt_delete(_arip->bt_orig);
        _arip->bt_orig = NULL;
        _arip->bt = NULL;
        _arip->key = 0;
      }
    }
    else
    {
      // This could have been the bucket we left off on
      // last time...if so, then we need to reset
      // _arip->key to start at the beginning of
      // the next valid hash bucket.
      _arip->key = 0;
    }
  }

  // If bt is NULL, then we either hit an error above, or
  // we didn't find a key.
  if (_arip->bt == NULL) {

    // If rc == 0, that means we didn't find a key.  Set
    // rc to EAGAIN so that the caller can try with the next
    // thread.
    if (rc == 0)
    {
      _arip->key = 0;
      _arip->hpos = 0;
      rcbp->rc = EAGAIN;
    }
    rcbp->res = -1;
    goto ark_next_pool_err;
  }

  // We have our bucket and now we need to find the next key.
  pos = _arip->bt->data;

  kcnt = 0;
  do
  {
    pos += vi_dec64(pos, &pkl);
    pos += vi_dec64(pos, &pvl);

    // Have you found our key place in the bucket?
    if (kcnt == _arip->key)
    {
      break;
    }
    else
    {
      // Move to the next key/value pair in the bucket.
      // By here, we have made sure that we have enough keys
      // to find the _arip->key entry because of the check above.
      pos += (pkl + (pvl > _arip->bt->max ? _arip->bt->def : pvl));
      kcnt++;
    }
  } while (1);

  // Record the true length of the key
  rcbp->res = pkl;

  // Copy the key into the buffer for the passed in length
  memcpy(kbuf, pos, (pkl > rcbp->klen) ? rcbp->klen : pkl);

  // Are we done with this bucket or do we remember which key we
  // left off on for next call.
  _arip->key++;
  if (_arip->key == _arip->bt->cnt)
  {
    // If there are no more keys in this hash bucket
    // then jump to the next monitored hash bucket for this thread.
    _arip->hpos += _arkp->nthrds;
    _arip->key = 0;
  }

ark_next_pool_err:

  if (_arip->bt != NULL)
  {
    bt_delete(_arip->bt_orig);
    _arip->bt_orig = NULL;
    _arip->bt = NULL;
  }

  tcbp->state = ARK_CMD_DONE;
}

/**
 *******************************************************************************
 * \brief
 *  reduce memory footprint if possible after an io is complete
 ******************************************************************************/
void cleanup_task_memory(_ARK *_arkp, tcb_t *tcbp, int tid)
{
    if (tcbp->inb != NULL && tcbp->inblen > 2*_arkp->bsize)
    {
        KV_TRC_DBG(pAT,"REDUCE INB: tid:%d %"PRIu64" to %"PRIu64"",
                tid, tcbp->inblen, _arkp->bsize);
        bt_delete(tcbp->inb_orig);
        tcbp->inb = bt_new(0, _arkp->vlimit, sizeof(uint64_t),
                           &(tcbp->inblen), &(tcbp->inb_orig));
    }

    if (tcbp->oub != NULL && tcbp->oublen > 2*_arkp->bsize)
    {
        KV_TRC_DBG(pAT,"REDUCE OTB: tid:%d %"PRIu64" to %"PRIu64"",
                tid, tcbp->oublen, _arkp->bsize);
        bt_delete(tcbp->oub_orig);
        tcbp->oub = bt_new(0, _arkp->vlimit, sizeof(uint64_t),
                          &(tcbp->oublen), &(tcbp->oub_orig));
    }

    if (tcbp->vb != NULL && tcbp->vbsize > _arkp->bsize*ARK_MIN_VB)
    {
        KV_TRC_DBG(pAT,"REDUCE VAB: tid:%d %"PRIu64" to %"PRIu64"",
                tid, tcbp->vbsize, _arkp->bsize*ARK_MIN_VB);
        am_free(tcbp->vb_orig);
        tcbp->vbsize = _arkp->bsize*ARK_MIN_VB;
        tcbp->vb_orig = am_malloc(tcbp->vbsize);
        if (tcbp->vb_orig != NULL)
        {
            tcbp->vb = ptr_align(tcbp->vb_orig);
        }
        else
        {
            // Clear out the vbsize.  When this buffer is used
            // for a command, the command will fail gracefully
            tcbp->vbsize = 0;
            tcbp->vb = NULL;
        }
    }
}

/**
 *******************************************************************************
 * \brief
 ******************************************************************************/
int init_task_state(_ARK *_arkp, tcb_t *tcbp)
{
  rcb_t *rcbp = &(_arkp->rcbs[tcbp->rtag]);
  int init_state = 0;

  switch (rcbp->cmd)
  {
    case K_GET :
    {
      init_state = ARK_GET_START;
      break;
    }
    case K_SET :
    {
      init_state = ARK_SET_START;
      break;
    }
    case K_DEL :
    {
      init_state = ARK_DEL_START;
      break;
    }
    case K_EXI :
    {
      init_state = ARK_EXIST_START;
      break;
    }
    case K_RAND :
    {
      init_state = ARK_RAND_START;
      break;
    }
    case K_FIRST :
    {
      init_state = ARK_FIRST_START;
      break;
    }
    case K_NEXT :
    {
      init_state = ARK_NEXT_START;
      break;
    }
    default :
      // we should never get here
      KV_TRC_FFDC(pAT, "FFDC, bad rcbp->cmd: %d", rcbp->cmd);
      break;
  }

  return init_state;
}

/**
 *******************************************************************************
 * \brief
 *   function run by each ark thread
 ******************************************************************************/
void *pool_function(void *arg)
{
  PT      *pt     = (PT*)arg;
  int      id     = pt->id;
  _ARK    *_arkp  = pt->ark;
  scb_t   *scbp   = &(_arkp->poolthreads[id]);
  rcb_t   *iorcbp = NULL;
  rcb_t   *rcbp   = NULL;
  rcb_t   *rcbtmp = NULL;
  rcb_t   *ractp  = NULL;
  tcb_t   *iotcbp = NULL;
  tcb_t   *tcbp   = NULL;
  tcb_t   *tactp  = NULL;
  iocb_t  *iocbp  = NULL;
  queue_t *rq     = scbp->rqueue;
  queue_t *tq     = scbp->tqueue;
  queue_t *sq     = scbp->scheduleQ;
  queue_t *hq     = scbp->harvestQ;
  int32_t  i      = 0;
  int32_t  reqrc  = EAGAIN;
  int32_t  tskrc  = EAGAIN;
  int32_t  tskact = 0;
  int32_t  reqact = 0;
  int32_t  itag   = 0;
  int32_t  reqtag = -1;
  int32_t  tsktag = 0;
  uint64_t hval   = 0;
  uint64_t hlba   = 0;
  uint64_t hsleep = 0;
  uint32_t MAX_POLL=0;
  uint64_t perfT  = 0;
  uint64_t verbT  = 0;
  int      max    = 0;

  KV_TRC_DBG(pAT, "start tid:%d nactive:%d", id, _arkp->nactive);

  /*----------------------------------------------------------------------------
   * Run until the thread state is EXIT or the global
   * state, ark_exit, is not set showing we are shutting
   * down the ark db.
   *--------------------------------------------------------------------------*/
  while ((scbp->poolstate != PT_EXIT))
  {
      /*------------------------------------------------------------------------
       * harvest IOs
       *----------------------------------------------------------------------*/
      MAX_POLL = queue_count(hq);
      if (MAX_POLL)
      {
          MAX_POLL = MAX_POLL <  10 ? 1 : MAX_POLL/10;
      }
      for (i=0; i<MAX_POLL; i++)
      {
          if (queue_deq_unsafe(hq, &itag) != 0) {continue;}
          iotcbp = &(_arkp->tcbs[itag]);
          iocbp  = &(_arkp->iocbs[itag]);
          iorcbp = &(_arkp->rcbs[iotcbp->rtag]);

          KV_TRC_EXT3(pAT, "HQ_DEQ  tid:%d ttag:%3d state:%d",
                     id, itag, iotcbp->state);

          if (iotcbp->state == ARK_IO_HARVEST)
          {
              ea_async_io_harvest(_arkp, id, iotcbp, iocbp, iorcbp);
              if (iocbp->hmissN) {++hsleep;}
          }

          // place the iocb on a queue for the next step
          if      (iotcbp->state==ARK_IO_SCHEDULE) {queue_enq_unsafe(sq, itag);}
          else if (iotcbp->state==ARK_IO_HARVEST)  {queue_enq_unsafe(hq, itag);}
          else                                     {queue_enq_unsafe(tq, itag);}
      }

      /*------------------------------------------------------------------------
       * schedule IOs
       *----------------------------------------------------------------------*/
      for (i=0; i<queue_count(sq); i++)
      {
          if (queue_deq_unsafe(sq, &itag) != 0) {continue;}
          iotcbp = &(_arkp->tcbs[itag]);
          iocbp  = &(_arkp->iocbs[itag]);
          iorcbp = &(_arkp->rcbs[iotcbp->rtag]);

          KV_TRC_EXT3(pAT, "SQ_DEQ  tid:%d ttag:%3d state:%d",
                     id, itag, iotcbp->state);

          if (iotcbp->state == ARK_IO_SCHEDULE)
              {ea_async_io_schedule(_arkp, id, iotcbp, iocbp);}

          // place the iocb on a queue for the next step
          if      (iotcbp->state==ARK_IO_SCHEDULE) {queue_enq_unsafe(sq, itag);}
          else if (iotcbp->state==ARK_IO_HARVEST)  {queue_enq_unsafe(hq, itag);}
          else                                     {queue_enq_unsafe(tq, itag);}
      }

      /*------------------------------------------------------------------------
       * usleep if appropriate
       *----------------------------------------------------------------------*/
      if (hsleep>=1000 && queue_count(rq)==0 && queue_count(tq)==0)
      {
          hsleep=0;
          usleep(5);
          KV_TRC_DBG(pAT,"IO:     USLEEP");
      }

      /*------------------------------------------------------------------------
       * check the request queue and try to pull off
       * as many requests as possible and queue them up
       * in the task queue
       *----------------------------------------------------------------------*/
      queue_lock(rq);
      if (queue_empty(rq) && reqtag == -1)
      {
          if (queue_empty(sq) && queue_empty(hq) && queue_empty(tq))
          {
              // We have reached a point where there is absolutely
              // no work for this worker thread to do.  So we
              // go to sleep waiting for new requests to come in
              KV_TRC_IO(pAT, "IO:     tid:%d IDLE_THREAD", id);
              queue_wait(rq);
          }
      }
      while (((reqrc == EAGAIN) && !(queue_empty(rq))) ||
             ((reqrc == 0) && (!tag_empty(_arkp->ttags))))
      {
          if (reqrc == EAGAIN)
          {
            reqrc = queue_deq_unsafe(rq, &reqtag);
            if ( reqrc == 0 )
            {
              rcbp = &(_arkp->rcbs[reqtag]);
              rcbp->rtag = reqtag;
              rcbp->ttag = -1;
              rcbp->hold = -1;
            }
            KV_TRC_EXT3(pAT, "RQ_DEQ  tid:%d ttag:%3d", id, reqtag);
          }

          if (reqrc == 0)
          {
            hval = HASH_GET(_arkp->ht, rcbp->pos);
            if (HASH_LCK(hval))
            {
              tsktag = HASH_TAG(hval);
              tcbp = &(_arkp->tcbs[tsktag]);
              rcbtmp = &(_arkp->rcbs[tcbp->rtag]);
              while (rcbtmp->hold != -1)
              {
                rcbtmp = &(_arkp->rcbs[rcbtmp->hold]);
              }
              scbp->holds++;
              rcbtmp->hold = reqtag;
              reqrc = EAGAIN;
              reqtag = -1;
              rcbp = NULL;
              KV_TRC_DBG(pAT,"IO:     tid:%2d RQ_EAGAIN rq:%3d tq:%2d sq:%2d "
                             "hq:%2d", id, rq->c, tq->c, sq->c, hq->c);
            }
            else
            {
              tskrc = tag_unbury(_arkp->ttags, &tsktag);
              if (tskrc == 0) {tcbp = &(_arkp->tcbs[tsktag]);}
              else            {tcbp = NULL;}

              if (tcbp)
              {
                tcbp->rtag = reqtag;
                rcbp->ttag = tsktag;
                rcbp->hold = -1;
                tcbp->state = init_task_state(_arkp, tcbp);
                tcbp->sthrd = rcbp->sthrd;
                tcbp->ttag = tsktag;
                tcbp->new_key = 0;
                hlba = HASH_LBA(hval);
                HASH_SET(_arkp->ht, rcbp->pos, HASH_MAKE(1, tsktag, hlba));
                (void)queue_enq_unsafe(tq, tsktag);
                KV_TRC_DBG(pAT, "IO:     tid:%d ttag:%3d rtag:%d TQ_ENQ START ",
                           id, tsktag, reqtag);
                reqtag = -1;
                reqrc = EAGAIN;
                tskrc = EAGAIN;
                rcbp = NULL;
                tcbp = NULL;
              }
            }
          }
      }
      queue_unlock(rq);

      /* do calcs for avg QD */
      _arkp->QDT += tq->c + rq->c + sq->c + hq->c;
      _arkp->QDN += 1;

      /*------------------------------------------------------------------------
       * initiate or continue tasks
       *----------------------------------------------------------------------*/
      max = tq->c;
      for (i=0; i<max; i++)
      {
          int s=0;
          if (queue_deq_unsafe(tq, &tskact) != 0) {continue;}

          tactp  = &(_arkp->tcbs[tskact]);
          reqact = tactp->rtag;
          ractp  = &(_arkp->rcbs[reqact]);
          s      = tactp->state;

          KV_TRC_EXT3(pAT, "TQ_DEQ  tid:%d ttag:%3d state:%d ",
                  id, tskact, s);

          if      (s==ARK_SET_START)      {ark_set_start      (_arkp,id,tactp);}
          else if (s==ARK_SET_PROCESS_INB){ark_set_process_inb(_arkp,id,tactp);}
          else if (s==ARK_SET_WRITE)      {ark_set_write      (_arkp,id,tactp);}
          else if (s==ARK_SET_FINISH)     {ark_set_finish     (_arkp,id,tactp);}
          else if (s==ARK_GET_START)      {ark_get_start      (_arkp,id,tactp);}
          else if (s==ARK_GET_PROCESS)    {ark_get_process    (_arkp,id,tactp);}
          else if (s==ARK_GET_FINISH)     {ark_get_finish     (_arkp,id,tactp);}
          else if (s==ARK_DEL_START)      {ark_del_start      (_arkp,id,tactp);}
          else if (s==ARK_DEL_PROCESS)    {ark_del_process    (_arkp,id,tactp);}
          else if (s==ARK_DEL_FINISH)     {ark_del_finish     (_arkp,id,tactp);}
          else if (s==ARK_EXIST_START)    {ark_exist_start    (_arkp,id,tactp);}
          else if (s==ARK_EXIST_FINISH)   {ark_exist_finish   (_arkp,id,tactp);}
          else if (s==ARK_RAND_START)     {ark_rand_pool      (_arkp,id,tactp);}
          else if (s==ARK_FIRST_START)    {ark_first_pool     (_arkp,id,tactp);}
          else if (s==ARK_NEXT_START)     {ark_next_pool      (_arkp,id,tactp);}

          /*--------------------------------------------------------------------
           * schedule the first IO right way
           *------------------------------------------------------------------*/
          if (tactp->state == ARK_IO_SCHEDULE)
          {
              iocbp = &(_arkp->iocbs[tskact]);
              ea_async_io_schedule(_arkp, id, tactp, iocbp);
          }

          /*--------------------------------------------------------------------
           * requeue task
           *------------------------------------------------------------------*/
          if (tactp->state == ARK_IO_SCHEDULE)
          {
              KV_TRC_EXT3(pAT, "SQ_ENQ  tid:%d ttag:%3d state:%d ",
                          id, tskact, tactp->state);
              queue_enq_unsafe(sq,tskact);
          }
          else if (tactp->state == ARK_IO_HARVEST)
          {
              KV_TRC_EXT3(pAT, "HQ_ENQ  tid:%d ttag:%3d state:%d ",
                          id, tskact, tactp->state);
              queue_enq_unsafe(hq,tskact);
          }
          else
          {
              KV_TRC_EXT3(pAT, "TQ_ENQ  tid:%d ttag:%3d state:%d ",
                          id, tskact, tactp->state);
              (void)queue_enq_unsafe(tq, tskact);
          }
      }

      /*------------------------------------------------------------------------
       * complete requests
       *----------------------------------------------------------------------*/
      max = tq->c;
      for (i=0; i<max; i++)
      {
          if (queue_deq_unsafe(tq, &tskact) != 0) {continue;}

          tactp  = &(_arkp->tcbs[tskact]);
          reqact = tactp->rtag;
          ractp  = &(_arkp->rcbs[reqact]);

          KV_TRC_EXT3(pAT, "TQ_DEQ  tid:%d ttag:%3d state:%d ",
                      id, tskact, tactp->state);

          if (tactp->state == ARK_CMD_DONE)
          {
              uint32_t lat = UDELTA(ractp->stime, _arkp->ns_per_tick);
              UPDATE_LAT(_arkp, ractp, lat);

              cleanup_task_memory(_arkp, tactp, id);

              if (ractp->hold == -1)
              {
                  hlba = HASH_LBA(HASH_GET(_arkp->ht, ractp->pos));
                  HASH_SET(_arkp->ht, ractp->pos, HASH_MAKE(0, 0, hlba));
                  if ( ractp->cb != NULL )
                  {
                      KV_TRC(pAT, "CMD_CMP tid:%d ttag:%3d rtag:%3d lat:%6d",
                              id, tskact, ractp->rtag, lat);
                      (ractp->cb)(ractp->rc, ractp->dt, ractp->res);
                      ractp->stat = A_NULL;
                      (void)tag_bury(_arkp->rtags, reqact);
                  }
                  else
                  {
                      pthread_mutex_lock(&(ractp->alock));
                      ractp->stat = A_COMPLETE;
                      pthread_cond_broadcast(&(ractp->acond));
                      pthread_mutex_unlock(&(ractp->alock));
                      KV_TRC(pAT, "CMD_CMP tid:%d ttag:%3d rtag:%3d lat:%6d",
                              id, tskact, ractp->rtag, lat);
                  }
                  (void)tag_bury(_arkp->ttags, tskact);
              }
              else
              {
                  tactp->rtag = ractp->hold;
                  ractp->hold = -1;
                  if ( ractp->cb != NULL)
                  {
                      KV_TRC(pAT, "CMD_CMP tid:%d ttag:%3d rtag:%3d lat:%6d",
                                id, tskact, ractp->rtag, lat);
                      (ractp->cb)(ractp->rc, ractp->dt, ractp->res);
                      ractp->stat = A_NULL;
                      (void)tag_bury(_arkp->rtags, reqact);
                  }
                  else
                  {
                      pthread_mutex_lock(&(ractp->alock));
                      ractp->stat = A_COMPLETE;
                      pthread_cond_broadcast(&(ractp->acond));
                      pthread_mutex_unlock(&(ractp->alock));
                      KV_TRC(pAT, "CMD_CMP tid:%d ttag:%3d rtag:%3d lat:%6d",
                              id, tskact, ractp->rtag, lat);
                  }

                  scbp->holds--;
                  ractp        = &(_arkp->rcbs[tactp->rtag]);
                  ractp->ttag  = tactp->ttag;
                  tactp->rtag  = ractp->rtag;
                  tactp->state = init_task_state(_arkp, tactp);
                  (void)queue_enq_unsafe(tq, tskact);
                  KV_TRC_EXT3(pAT, "TQ_ENQ  tid:%d ttag:%3d state:%d",
                              id, tskact, tactp->state);
              }
              continue;
          }
          else
          {
              (void)queue_enq_unsafe(tq, tskact);
              KV_TRC_EXT3(pAT, "TQ_ENQ  tid:%d ttag:%3d state:%d ",
                      id, tskact, tactp->state);
          }
      }

      /*------------------------------------------------------------------------
       * performance and dynamic tracing
       *----------------------------------------------------------------------*/
      if (SDELTA(perfT, _arkp->ns_per_tick) > 1)
      {
          uint64_t opsT = _arkp->set_opsT + _arkp->get_opsT + _arkp->exi_opsT +\
                          _arkp->del_opsT;
          uint64_t latT = _arkp->set_latT + _arkp->get_latT + _arkp->exi_latT +\
                          _arkp->del_latT;
          uint64_t lat=0,set_lat=0,get_lat=0,exi_lat=0,del_lat=0,QDA=0;
          if (opsT)            {lat     = latT/opsT;}
          if (_arkp->set_opsT) {set_lat = _arkp->set_latT/_arkp->set_opsT;}
          if (_arkp->get_opsT) {get_lat = _arkp->get_latT/_arkp->get_opsT;}
          if (_arkp->exi_opsT) {exi_lat = _arkp->exi_latT/_arkp->exi_opsT;}
          if (_arkp->del_opsT) {del_lat = _arkp->del_latT/_arkp->del_opsT;}
          if (_arkp->QDT)      {QDA     = _arkp->QDT/_arkp->QDN;}
          KV_TRC_PERF1(pAT,"IO:     tid:%d PERF   opsT:%7ld lat:%7ld "
                           "opsT(%7ld %7ld %7ld %7ld) "
                           "lat(%7ld %6ld %6ld %6ld) QD:%4ld issT:%4d",
                           id, opsT, lat,
                           _arkp->set_opsT, _arkp->get_opsT, _arkp->exi_opsT,
                           _arkp->del_opsT,
                           set_lat, get_lat, exi_lat, del_lat,
                           QDA, _arkp->issT);
          KV_TRC_PERF2(pAT,"IO:     tid:%d QUEUES rq:%3d tq:%3d sq:%4d hq:%4d ",
                           id, rq->c, tq->c, sq->c, hq->c);
          _arkp->set_latT = 0;
          _arkp->get_latT = 0;
          _arkp->exi_latT = 0;
          _arkp->del_latT = 0;
          _arkp->set_opsT = 0;
          _arkp->get_opsT = 0;
          _arkp->exi_opsT = 0;
          _arkp->del_opsT = 0;
          _arkp->QDT      = 0;
          _arkp->QDN      = 0;
          perfT           = getticks();

          /* check for dynamic verbosity every 5 seconds */
          if (SDELTA(verbT, _arkp->ns_per_tick) > 5)
          {
              struct stat sbuf;
              FILE  *fp        = NULL;
              char   verbS[2]  = {0};
              int    verbI     = 0;

              verbT = getticks();

              if (stat(ARK_VERB_FN,&sbuf) == 0)
              {
                  if ((fp=fopen(ARK_VERB_FN,"r")))
                  {
                      if (fgets(verbS, 2, fp))
                      {
                          verbI = atoi(verbS);
                          if (verbI > 0 && verbI < 10) {pAT->verbosity=verbI;}
                      }
                      fclose(fp);
                  }
              }
          }
      }
  }

  KV_TRC(pAT, "exiting tid:%d nactive:%d", id, _arkp->nactive);
  return NULL;
}

