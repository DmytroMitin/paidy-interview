<img src="/paidy.png?raw=true" width=300 style="background-color:white;">

# Local proxy for currency exchange rates

Host, port of local proxy and host, port, token of One-Frame service can be configured in `src/main/resources/application.conf`. For example,
```
app {
  http {
    host = "0.0.0.0"
    port = 8081
    timeout = 40 seconds
  }
  one-frame {
    host = "0.0.0.0"
    port = 8080
    token = 10dc303535874aeccc86a8251e6992f5
  }
}
```

One-Frame service can be started with
```
docker pull paidyinc/one-frame
docker run -p 8080:8080 paidyinc/one-frame
```

Local proxy can be started with `sbt run`
Requests look like http://localhost:8081/rates?from=USD&to=JPY

Tests can be run with `sbt test`