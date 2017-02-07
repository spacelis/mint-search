package uk.ac.cdrc.mintsearch.neo4j;

import org.neo4j.graphdb.Node;

/**
 * This is a legacy code from Neo4J example
 */
public class SearchHit{
    public Long nodeId;
    public SearchHit(Node node){
        this.nodeId = node.getId();
    }

}
