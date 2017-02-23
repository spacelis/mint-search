Randomly Generated Person Address Data
======================================

This data set is a randomly generated via [neofaker](https://github.com/spacelis/neofaker) which is a python library based on [faker](https://github.com/joke2k/faker).


Example Queries
===============

1. Moving with one person changed name

```
create (ra: Record)-[:Surname]->(:Name {value: 'Middleton'}),
       (ra)-[:FORENAME]->(:Name {value: 'Michael'}),
       (ra)-[:LIVE_AT]->(a: Address {value: '4 London Road'}),
       (rb: Record)-[:SURNAME]->(:Name {value: 'Middleton'}),
       (rb)-[:FORENAME]->(:Name {value: 'Vincent'}),
       (rb)-[:LIVE_AT]->(a)
```
