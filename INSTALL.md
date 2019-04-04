
# build and test 
```
lein do clean, test, uberjar
```

# manual test cases in [here](TESTING.md)


# configure

`cp config.edn_template config.edn` and fill in API keys and other entries


# run locally
```
lein run
```

or overwrite configuration
```
CONFIG_FILE=custom_config.edn lein run
```

# run in production
```
java -Djava.security.manager -Djava.security.policy==java.policy -jar target/CLOVER.jar
```

or with diagnostic repl and custom config
```
CONFIG_FILE=custom_config.edn java -Djava.security.manager -Djava.security.policy==java.policy -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar target/CLOVER.jar
```
