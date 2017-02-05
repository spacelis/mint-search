package uk.ac.cdrc.mintsearch.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.List;

/**
 * This class is used for return Graph results, copied from
 * apoc.result.GraphResult (@https://github.com/neo4j-contrib/neo4j-apoc-procedures)
 */
public class GraphResult {
    public final List<Node> nodes;
    public final List<Relationship> relationships;

    public GraphResult(List<Node> nodes, List<Relationship> relationships) {
        this.nodes = nodes;
        this.relationships = relationships;
    }
}
