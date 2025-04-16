# edn-comparator

Compare 2 edn files.

WARNING: We're only interested in comparing keys to check for their presence or absence.

Usage:

bb compare_edns.bb <file1.edn> <file2.edn>

Result:

Shows which keys are missing and/or are extra in <file1.edn> in comparison with <file2.edn>

Example:

sample1.edn

```clojure
{:a 1
 :x 'Fiona!
 :y [{:yy 43}]}
```

sample2.edn

```clojure
{:a "Scotty doesn't know"
 :x 42
 :y [{:k 99}]
 :z "hi"}
```

Results:

```bash
bb compare_edns.bb sample1.edn sample2.edn
Comparing files sample1.edn and sample2.edn
```clojure
{:y #{+"0.:yy" -"0.:k"}, -:z #{}}
```
```
