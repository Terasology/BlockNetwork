/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.blockNetwork.traversal;

/**
 * Visitor method for breadth-first traversal of a graph.
 * This method is invoked for each node in a traversal, as directed by the traversal definition and return value from
 * the method.
 * @param <T> Type of nodes in the graph.
 * @param <U> Return value type from the traversal.
 * @param <V> Value passed from node visited into nodes that are outgoing from that node.
 */
public interface BreadthFirstTraversal<T, U, V> {
    /**
     * Traverses the given node in the graph.
     * This method is invoked for each node in a traversal as directed by the traversal definition and the traversal result.
     * 
     * @param node The type of nodes in the graph.
     * @param parentValue The value passed from the parent node in the traversal into this node.
     * @return The result of the node traversal.
     */
    TraversalResult<T, U, V> visitNode(T node, V parentValue);
}
