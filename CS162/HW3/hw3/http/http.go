package http

import (
	"errors"
	"fmt"
	"net"
	"os"
	"strings"
)

const maxRequestSize = 8192

type Request struct {
	Method string
	Path   string
}

func ParseRequest(connection net.Conn) (*Request, error) {
	payload := make([]byte, maxRequestSize)
	_, err := connection.Read(payload)
	if err != nil {
		errMsg := fmt.Sprintf("Failed to read HTTP request: %s", err)
		fmt.Fprintln(os.Stderr, errMsg)
		return nil, errors.New(errMsg)
	}

	payloadStr := string(payload)
	splitPoint := strings.Index(payloadStr, "\r\n")
	if splitPoint == -1 {
		return nil, errors.New("Invalid HTTP request method and path")
	}
	firstLine := payloadStr[:splitPoint]

	tokens := strings.SplitN(firstLine, " ", 3)
	return &Request{
		Method: tokens[0],
		Path:   tokens[1],
	}, nil
}

func getResponseMessage(statusCode int) string {
	switch statusCode {
	case 100:
		return "Continue"
	case 200:
		return "OK"
	case 301:
		return "Moved Permanently"
	case 302:
		return "Found"
	case 304:
		return "Not Modified"
	case 400:
		return "Bad Request"
	case 401:
		return "Unauthorized"
	case 403:
		return "Forbidden"
	case 404:
		return "Not Found"
	case 405:
		return "Method Not Allowed"
	default:
		return "Internal Server Error"
	}
}

func StartResponse(connection net.Conn, statusCode int) {
	_, err := fmt.Fprintf(connection, "HTTP/1.0 %d %s\r\n", statusCode, getResponseMessage(statusCode))
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to write response: %s\n", err)
		os.Exit(1)
	}
}

func SendHeader(connection net.Conn, key string, value string) {
	_, err := fmt.Fprintf(connection, "%s: %s\r\n", key, value)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to write HTTP header: %s\n", err)
		os.Exit(1)
	}
}

func EndHeaders(connection net.Conn) {
	_, err := fmt.Fprintf(connection, "\r\n")
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to write end to HTTP headers: %s\n", err)
		os.Exit(1)
	}
}

func SendString(connection net.Conn, msg string) {
	_, err := fmt.Fprintf(connection, msg)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to write string: %s\n", err)
		os.Exit(1)
	}
}

func SendData(connection net.Conn, data []byte) {
	_, err := connection.Write(data)
	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to write data: %s\n", err)
		os.Exit(1)
	}
}

func GetMimeType(fileName string) string {
	splitPoint := strings.LastIndex(fileName, ".")
	if splitPoint == -1 {
		return "text/plain"
	}

	extension := fileName[splitPoint+1:]
	switch extension {
	case "html":
		return "text/html"
	case "jpg":
		return "image/jpeg"
	case "png":
		return "image/png"
	case "css":
		return "text/css"
	case "js":
		return "application/javascript"
	case "pdf":
		return "application/pdf"
	default:
		return "text/plain"
	}
}
