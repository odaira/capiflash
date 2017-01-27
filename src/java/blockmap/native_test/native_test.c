/* IBM_PROLOG_BEGIN_TAG
 * This is an automatically generated prolog.
 *
 * $Source: src/java/blockmap/native_test/native_test.c $
 *
 * IBM Data Engine for NoSQL - Power Systems Edition User Library Project
 *
 * Contributors Listed Below - COPYRIGHT 2015,2016,2017
 * [+] International Business Machines Corp.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * IBM_PROLOG_END_TAG
 */

// @author Jan S. Rellermeyer, IBM Research

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <string.h>
#include <assert.h>
#include <errno.h>

#include "arkdb.h"

#define WORKERS 4
#define REP 100

#define NASYNC 128

#define KEY_LEN 50
#define VAL_LEN 3324

#define TRACE_ENABLE 1 

#if TRACE_ENABLE
#define TRACE(...)	fprintf(stderr, "[TRACE] %s(%d): ",__FILE__, __LINE__ ); fprintf(stderr, __VA_ARGS__);
#else
#define TRACE(...)
#endif

//#define DEVICE (char*) NULL
//#define DEVICE "/dev/hdisk0"
#define DEVICE "/dev/sdf"

struct work {
	ARK *ark;
	int rep;
};

void rand_str(char *dest, size_t length) {
    char charset[] = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    while (length-- > 0) {
        size_t index = (double) rand() / RAND_MAX * (sizeof charset - 1);
        *dest++ = charset[index];
    }
    *dest = '\0';
}

typedef struct
{
	int errcode;
	int64_t res;
	uint64_t condvar;
} cb_info __attribute__ ((aligned(16)));

static void callback(int errcode, uint64_t dt, int64_t res)
{
	cb_info *s = (cb_info*) dt;

	assert(s->condvar == 0);

	TRACE("received callback, errcode=%d, res=%ld, condvar=%lu\n", errcode, res, s->condvar);

	s->errcode = errcode;
	s->res = res;
	s->condvar = 1;

	__sync_synchronize();
}

void *worker(void* state)
{
	ARC *arc;
	int rc;

	struct work *work_item = (struct work*) state;

	rc = ark_connect_verbose(&arc, (ARK*) work_item->ark, NASYNC);
	assert(rc == 0 && "ark_connect_verbose");

	char key[KEY_LEN];
	char val[VAL_LEN];

	for (int i=0; i<work_item->rep; i++)
	{
		cb_info s;
		memset(&s, 0, sizeof(cb_info));

		rand_str((char*) &key, KEY_LEN);
		int size = rand() % VAL_LEN;
		rand_str((char*) &val, size);

		for (int j=0; j<2; j++) {
		assert(s.condvar == 0);
		rc = ark_set_async_cb(arc, KEY_LEN, (void*) key, size, (void*) val, &callback,
					(uint64_t) &s);
		assert(rc == 0 && "ark_set");

		while (!__sync_bool_compare_and_swap(&s.condvar, 1, 0))
		{
			// spin
		}

		assert(s.errcode == 0);
		assert(s.res == size);
		}
	}

	return 0;
}


int main(void)
{
	fprintf(stderr, "[RUNNING TEST]\n");

	// seed the random generator
	unsigned seed = time(NULL);
	srand(seed);

	// connect to device
	ARK *ark;

	int rc = ark_create(DEVICE, &ark, 0);

	assert(rc == 0 && "ark_create_verbose");

	struct work work_items[WORKERS];
	pthread_t threads[WORKERS];

	for (int i=0; i<WORKERS; i++)
	{
		work_items[i].ark = ark;
		work_items[i].rep = REP;

		pthread_create(&threads[i], NULL, worker, (void*) &work_items[i]);
	}

	for (int i=0; i<WORKERS; i++)
	{
		pthread_join(threads[i], NULL);
	}

	fprintf(stderr, "[SUCCESS]\n");

	return 0;
}
