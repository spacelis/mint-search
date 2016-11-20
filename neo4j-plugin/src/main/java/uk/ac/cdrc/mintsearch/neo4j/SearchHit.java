package uk.ac.cdrc.mintsearch.neo4j;

/**
 * Created by ucfawli on 19-Nov-16.
 */


import org.neo4j.graphdb.Node;

/**
 * This is the output record for our search procedure. All procedures
 * that return results return them as a Stream of Records, where the
 * records are defined like this one - customized to fit what the procedure
 * is returning.
 *
 * These classes can only have public non-final fields, and the fields must
 * be one of the following types:
 *
 * <ul>
 *     <li>{@link String}</li>
 *     <li>{@link Long} or {@code long}</li>
 *     <li>{@link Double} or {@code double}</li>
 *     <li>{@link Number}</li>
 *     <li>{@link Boolean} or {@code boolean}</li>
 *     <li>{@link org.neo4j.graphdb.Node}</li>
 *     <li>{@link org.neo4j.graphdb.Relationship}</li>
 *     <li>{@link org.neo4j.graphdb.Path}</li>
 *     <li>{@link java.util.Map} with key {@link String} and value {@link Object}</li>
 *     <li>{@link java.util.List} of elements of any valid field type, including {@link java.util.List}</li>
 *     <li>{@link Object}, meaning any of the valid field types</li>
 * </ul>
 */
public class SearchHit{
    public Long nodeId;
    public SearchHit(Node node){
        this.nodeId = node.getId();
    }

}
