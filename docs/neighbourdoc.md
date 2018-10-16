Neighbour as Document
=====================

Neighbour Document is a project for graph searching which focus on fuzzy matchings.
Suppose you have two graphs or two snapshots of ever evolving graph and you need to infer the identity of each nodes in the two graphs.
The traditional graph search by checking the isomorphism would not work since it is the change need to be tracking.
The edit-distance will sort of work but may not capture the importance of the edges in the changes/stays.
Simrank for graph nodes are not designed to find identification as well as NESS (Neighbour Based Graph Search).
However the latter provoke a good perspective in viewing this problem, i.e., looking at the nodes as a document of neighbours.
In this we we can describe a node by its neighbours or the chain of neighbours.
We can describe nodes by its direct neighbours and second neighbours just like the information propagating principal in NESS.
As in the domain of IR, document fuzzy searching have been studied for decades as well as the approximation of the contribution of fragments in documents.
Thus it would be interesting to look at the effectiveness of reducing the graph search problem to document search problem.


Approach
--------

- Neo4J is an open-source graph database that is equipped with extensible API so that customized procedure for manipulating indices can be programmed and integrated via plugins.
- Neo4J has legacy support of Lucene indexing (both exact and full-text indexing) which simplify the code to build customized index for specific algorithm.


Steps
-----
- The first step will be implementing the NESS to demonstrate its performance in the task of person identity tracking in CR.
- The second step will be combining the neighbour document with different IR scoring methods such as tf-idf/bm25.
- The performance of the two will be compared and insights will be drawn.


Datasets
--------

- CR
- Web Graph (uk-2007-05, also used in NESS paper)
- Freebase (freebase-rdf-lastest, also used in NESS paper)
- DBLP Collaboration Network (also used in NESS paper)


Contribution
------------
1. Pose a new way of tackling graph search, especially useful when the graph is not totally accurate nor reliable.
2. Compared the NESS with the Neighbour-as-document in both theory and practice.
3. A set of plugins for Neo4J that implemented the algorithm related to this project.
