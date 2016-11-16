create (a:N {labels: "a"}),
       (b:N {labels: "b"}),
       (c:N {labels: "c"}),
       (d:N {labels: "d"}),
       (w:N {labels: "w"}),
       (x:N {labels: "x"}),
       (y:N {labels: "y"}),
       (z:N {labels: "z"}),
       (a)-[:relateTo]->(b),
       (b)-[:relateTo]->(d),
       (d)-[:relateTo]->(c),
       (c)-[:relateTo]->(a),
       (a)-[:relateTo]->(d),
       (c)-[:relateTo]->(b),
       (w)-[:relateTo]->(a),
       (x)-[:relateTo]->(b),
       (y)-[:relateTo]->(d),
       (z)-[:relateTo]->(c);
call mint.ness_index(0, "relateTo", "labels", 1, 0.1);
call mint.ness_index(1, "relateTo", "labels", 1, 0.1);
call mint.ness_index(2, "relateTo", "labels", 1, 0.1);
call mint.ness_index(3, "relateTo", "labels", 1, 0.1);
call mint.ness_index(4, "relateTo", "labels", 1, 0.1);
call mint.ness_index(5, "relateTo", "labels", 1, 0.1);
call mint.ness_index(6, "relateTo", "labels", 1, 0.1);
call mint.ness_index(7, "relateTo", "labels", 1, 0.1);
