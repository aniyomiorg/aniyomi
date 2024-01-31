package utils

import (
	"sync"
)

func ParallelFor(begin, end int, fn func(i int)) {
	var wg sync.WaitGroup
	wg.Add(end - begin)
	for i := begin; i < end; i++ {
		go func(i int) {
			fn(i)
			wg.Done()
		}(i)
	}
	wg.Wait()
}
