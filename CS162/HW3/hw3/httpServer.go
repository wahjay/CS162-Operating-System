package main

import (
	"./http"
	"fmt"
	"io/ioutil"
	"net"
	"os"
        "log"
	"os/signal"
	"strconv"
	"strings"
	"sync"
)

const usageMsg = "Usage: ./httpserver --files www_directory/ --port 8000 [--num-threads 5]\n" +
	"       ./httpserver --proxy inst.eecs.berkeley.edu:80 --port 8000 [--num-threads 5]\n"
const maxQueueSize = 50

var (
	serverFilesDirectory string
	proxyAddress         string
	proxyPort            int

	interrupted chan os.Signal
)

func handleFilesRequest(connection net.Conn) {
    request, err := http.ParseRequest(connection)
    if err != nil {
        log.Fatal(err)
    }

    path := ""
    if request.Path == "/" {
        path = serverFilesDirectory
    } else {
        path = serverFilesDirectory + request.Path
    }

    file, err := os.Stat(path)
    if err != nil {
        http.StartResponse(connection, 404)
    }

    if !file.IsDir() {
        send_file_content(connection, path)
    } else if file.IsDir() {
       if is_exist(path) {
            send_file_content(connection, path)
       } else {
            send_subdir(connection, path)
       }

    } else {
        http.StartResponse(connection, 404)
    }
}

func handleProxyRequest(clientConn net.Conn) {
        
        port := strconv.Itoa(proxyPort)
        host_port := proxyAddress + ":" + port
        serverConn, err := net.Dial("tcp", host_port)
        if err != nil {
            log.Fatal(err)
        }

        var wg sync.WaitGroup
        wg.Add(2)
        Proxy_handler(clientConn, serverConn, &wg)
        wg.Wait()
}

func handleSigInt() {

    sig := <- interrupted
    switch sig {
        case os.Interrupt:
            fmt.Println("Got signal:", sig)
            os.Exit(0)
    }
}


func dispatch(requestHandler func(net.Conn), conn net.Conn, ch <-chan net.Conn) {

    for j := range ch {
        requestHandler(j)
    }

}

func initWorkerPool(numThreads int, requestHandler func(net.Conn), ch <-chan net.Conn) {
	// Create a fixed number of goroutines to handle requests

        //wg.Add(numThreads)
        for i := 0; i < numThreads; i++ {
            conn := <- ch
            go dispatch(requestHandler, conn, ch)
            //go requestHandler(ch)
        }
}

func serveForever(numThreads int, port int, requestHandler func(net.Conn)) {
        
        portNum := ":"
        portNum += strconv.Itoa(port)

        lis, err := net.Listen("tcp", portNum)
        if err != nil {
            fmt.Println("Listen failed.")
            log.Fatal(err)
        }
    
        //initWorkerPool(numThreads, requestHandler, ch)

        for {
            con, err := lis.Accept()
            if err != nil {
                fmt.Println("Accept failed.")
                log.Fatal(err)
            }
            requestHandler(con)
        }
}

func exitWithUsage() {
	fmt.Fprintf(os.Stderr, usageMsg)
	os.Exit(-1)
}

func main() {
	// Command line argument parsing
	var requestHandler func(net.Conn)
	var serverPort int
	numThreads := 1
	var err error

	for i := 1; i < len(os.Args); i++ {
		switch os.Args[i] {
		case "--files":
			requestHandler = handleFilesRequest
			if i == len(os.Args)-1 {
				fmt.Fprintln(os.Stderr, "Expected argument after --files")
				exitWithUsage()
			}
			serverFilesDirectory = os.Args[i+1]
			i++

		case "--proxy":
			requestHandler = handleProxyRequest
			if i == len(os.Args)-1 {
				fmt.Fprintln(os.Stderr, "Expected argument after --proxy")
				exitWithUsage()
			}
			proxyTarget := os.Args[i+1]
			i++

			tokens := strings.SplitN(proxyTarget, ":", 2)
			proxyAddress = tokens[0]
			proxyPort, err = strconv.Atoi(tokens[1])
			if err != nil {
				fmt.Fprintln(os.Stderr, "Expected integer for proxy port")
				exitWithUsage()
			}

		case "--port":
			if i == len(os.Args)-1 {
				fmt.Fprintln(os.Stderr, "Expected argument after --port")
				exitWithUsage()
			}

			portStr := os.Args[i+1]
			i++
			serverPort, err = strconv.Atoi(portStr)
			if err != nil {
				fmt.Fprintln(os.Stderr, "Expected integer value for --port argument")
				exitWithUsage()
			}

		case "--num-threads":
			if i == len(os.Args)-1 {
				fmt.Fprintln(os.Stderr, "Expected argument after --num-threads")
				exitWithUsage()
			}
			numThreadsStr := os.Args[i+1]
			i++
			numThreads, err = strconv.Atoi(numThreadsStr)
			if err != nil {
				fmt.Fprintln(os.Stderr, "Expected positive integer value for --num-threads argument")
				exitWithUsage()
			}

		case "--help":
			fmt.Printf(usageMsg)
			os.Exit(0)

		default:
			fmt.Fprintf(os.Stderr, "Unexpected command line argument %s\n", os.Args[i])
			exitWithUsage()
		}
	}

	if requestHandler == nil {
		fmt.Fprintln(os.Stderr, "Must specify one of either --files or --proxy")
		exitWithUsage()
	}
	
        // Set up a handler for SIGINT, used in Task #4
	interrupted = make(chan os.Signal, 1)
	signal.Notify(interrupted, os.Interrupt)
        //handleSigInt()
	serveForever(numThreads, serverPort, requestHandler)
}

func send_file_content(c net.Conn, path string){

    if strings.Compare(serverFilesDirectory, path) == 0 {
        path = serverFilesDirectory + "/index.html"
    }

    content, err := ioutil.ReadFile(path)
    if err != nil {
        log.Fatal(err)
    }
   
    length := ""
    length += strconv.Itoa(len(content))
    http.StartResponse(c, 200)
    http.SendHeader(c, "Content-Type", http.GetMimeType(path))
    http.SendHeader(c, "Content-Length", length)
    http.EndHeaders(c)
    http.SendData(c, content)
}


func is_exist(path string) bool {
    files, err := ioutil.ReadDir(path)
    if err != nil {
        log.Fatal(err)
    }

    for _, file := range files {
        if strings.Compare(file.Name(), "index.html") == 0 {
            return true
        }
    }

    return false
}

func send_subdir(c net.Conn, path string){
    files, err := ioutil.ReadDir(path)
    if err != nil {
        log.Fatal(err)
     }
    
    http.StartResponse(c, 200)
    http.SendHeader(c, "Content-Type", "text/html")
    http.EndHeaders(c)
    parent_dir := "<a href=../></a>"
    http.SendString(c, parent_dir)
    dir := ""
    
    //for e := files.Front(); e != nil; e = e.Next() {
    for i := 0; i < len(files); i++ {
        dir += "<a href="
        dir += files[i].Name()
        dir += "></a>"
        http.SendString(c, dir);
        dir = ""
    }
}

func Proxy_handler(clientConn net.Conn, serverConn net.Conn, wg *sync.WaitGroup){

    content := make([]byte, 4096)
    go func() {
        for {
            bytes_read, err := clientConn.Read(content)
            if err != nil {
                log.Fatal(err)
            }
            serverConn.Write(content[:bytes_read])
        }
    }()

    go func() {
        for {
            bytes_read, err := serverConn.Read(content)
            if err != nil {
                log.Fatal(err)
            }
            clientConn.Write(content[:bytes_read])
        }
    }()

   wg.Done()
}


