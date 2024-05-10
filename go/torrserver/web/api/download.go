package api

import (
	"fmt"
	"io"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

type fileReader struct {
	pos  int64
	size int64
	io.ReadSeeker
}

func newFR(size int64) *fileReader {
	return &fileReader{
		pos:  0,
		size: size,
	}
}

func (f *fileReader) Read(p []byte) (n int, err error) {
	f.pos = f.pos + int64(len(p))
	return len(p), nil
}

func (f *fileReader) Seek(offset int64, whence int) (int64, error) {
	switch whence {
	case 0:
		f.pos = offset
	case 1:
		f.pos += offset
	case 2:
		f.pos = f.size + offset
	}
	return f.pos, nil
}

// download godoc
//
//	@Summary		Generates test file of given size
//	@Description	Download the test file of given size (for speed testing purpose).
//
//	@Tags			API
//
//	@Param			size	path	string	true	"Test file size"
//
//	@Produce		application/octet-stream
//	@Success		200 {file} file
//	@Router			/download/{size} [get]
func download(c *gin.Context) {
	szStr := c.Param("size")
	sz, err := strconv.Atoi(szStr)
	if err != nil {
		c.Error(err)
		return
	}

	http.ServeContent(c.Writer, c.Request, fmt.Sprintln(szStr)+"mb.bin", time.Now(), newFR(int64(sz*1024*1024)))
}
