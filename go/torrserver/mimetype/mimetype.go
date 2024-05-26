package mimetype

import (
	"log"
	"mime"
	"net/http"
	"os"
	"path"
	"strings"
)

func init() {
	// Add a minimal number of mime types to augment go's built in types
	// for environments which don't have access to a mime.types file (e.g.
	// Termux on android)
	for _, t := range []struct {
		mimeType   string
		extensions string
	}{
		{"image/bmp", ".bmp"},
		{"image/gif", ".gif"},
		{"image/jpeg", ".jpg,.jpeg"},
		{"image/png", ".png"},
		{"image/tiff", ".tiff,.tif"},
		{"audio/x-aac", ".aac"},
		{"audio/dsd", ".dsd,.dsf,.dff"},
		{"audio/flac", ".flac"},
		{"audio/mpeg", ".mpga,.mpega,.mp2,.mp3,.m4a"},
		{"audio/ogg", ".oga,.ogg,.opus,.spx"},
		{"audio/opus", ".opus"},
		{"audio/weba", ".weba"},
		{"audio/x-ape", ".ape"},
		//		{"audio/x-dsd", ".dsd"},
		//		{"audio/x-dff", ".dff"},
		//		{"audio/x-dsf", ".dsf"},
		{"audio/x-wav", ".wav"},
		{"video/dv", ".dif,.dv"},
		{"video/fli", ".fli"},
		{"video/mp4", ".mp4"},
		{"video/mpeg", ".mpeg,.mpg,.mpe"},
		{"video/x-matroska", ".mpv,.mkv"},
		{"video/mp2t", ".ts,.m2ts,.mts"},
		{"video/ogg", ".ogv"},
		{"video/webm", ".webm"},
		{"video/x-ms-vob", ".vob"},
		{"video/x-msvideo", ".avi"},
		{"video/x-quicktime", ".qt,.mov"},
		{"text/srt", ".srt"},
		{"text/smi", ".smi"},
		{"text/ssa", ".ssa"},
	} {
		for _, ext := range strings.Split(t.extensions, ",") {
			err := mime.AddExtensionType(ext, t.mimeType)
			if err != nil {
				panic(err)
			}
		}
	}
	if err := mime.AddExtensionType(".rmvb", "application/vnd.rn-realmedia-vbr"); err != nil {
		log.Printf("Could not register application/vnd.rn-realmedia-vbr MIME type: %s", err)
	}
}

// Example: "video/mpeg"
type mimeType string

// IsMedia returns true for media MIME-types
func (mt mimeType) IsMedia() bool {
	return mt.IsVideo() || mt.IsAudio() || mt.IsImage()
}

// IsVideo returns true for video MIME-types
func (mt mimeType) IsVideo() bool {
	return strings.HasPrefix(string(mt), "video/") || mt == "application/vnd.rn-realmedia-vbr"
}

// IsAudio returns true for audio MIME-types
func (mt mimeType) IsAudio() bool {
	return strings.HasPrefix(string(mt), "audio/")
}

// IsImage returns true for image MIME-types
func (mt mimeType) IsImage() bool {
	return strings.HasPrefix(string(mt), "image/")
}

// IsSub returns true for subtitles MIME-types
func (mt mimeType) IsSub() bool {
	return strings.HasPrefix(string(mt), "text/srt") || strings.HasPrefix(string(mt), "text/smi") || strings.HasPrefix(string(mt), "text/ssa")
}

// Returns the group "type", the part before the '/'.
func (mt mimeType) Type() string {
	return strings.SplitN(string(mt), "/", 2)[0]
}

// Returns the string representation of this MIME-type
func (mt mimeType) String() string {
	return string(mt)
}

// MimeTypeByPath determines the MIME-type of file at the given path
func MimeTypeByPath(filePath string) (ret mimeType, err error) {
	ret = mimeTypeByBaseName(path.Base(filePath))
	if ret == "" {
		ret, err = mimeTypeByContent(filePath)
	}
	// Custom DLNA-compat mime mappings
	// TODO: make this depend on client headers / profile map
	if ret == "video/mp2t" {
		ret = "video/mpeg"
		//	} else if ret == "video/x-matroska" {
		//		ret = "video/mpeg"
	} else if ret == "video/x-msvideo" {
		ret = "video/avi"
	} else if ret == "" {
		ret = "application/octet-stream"
	}
	return
}

// Guess MIME-type from the extension, ignoring ".part".
func mimeTypeByBaseName(name string) mimeType {
	name = strings.TrimSuffix(name, ".part")
	ext := path.Ext(name)
	if ext != "" {
		return mimeType(mime.TypeByExtension(ext))
	}
	return mimeType("")
}

// Guess the MIME-type by analysing the first 512 bytes of the file.
func mimeTypeByContent(path string) (ret mimeType, err error) {
	file, err := os.Open(path)
	if err != nil {
		return
	}
	defer file.Close()
	var data [512]byte
	if n, err := file.Read(data[:]); err == nil {
		ret = mimeType(http.DetectContentType(data[:n]))
	}
	return
}
