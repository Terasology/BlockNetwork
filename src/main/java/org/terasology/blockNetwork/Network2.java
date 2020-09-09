// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import com.google.common.base.Predicate;
import org.terasology.blockNetwork.traversal.BreadthFirstTraversal;
import org.terasology.blockNetwork.traversal.BreadthFirstTraversalWithPath;

/**
 * Contains additional searching functionality for a network.
 *
 * @param <T> The type of node in this network.
 */
public interface Network2<T extends NetworkNode> extends Network<T> {
    /**
     * Allows breadth-first traversing of the network using the visitor passed. The visitor also receives the path from
     * the starting node to the node passed in visitor parameters.
     *
     * @param from Starting node, where the traversal starts.
     * @param traversal Visitor that gets called for each node during traversing.
     * @param defaultPredicate Default predicate to use when choosing which nodes and paths to follow, used if
     *         the call to visitor has not yielded a different Predicate for that path.
     * @param defaultResult Default result to return from the method, if none of the visitor calls yielded a
     *         result.
     * @param initialValue Initial value passed to the nodes in the first round of the breadth-first visitor
     *         calls.
     * @param <U> Result type returned by the visitor and this method.
     * @param <V> Value passed to the visitor for each call.
     * @return The result of the traversal or the defaultResult if none was returned by the visitor.
     */
    <U, V> U traverseBreadthFirstWithPath(T from, BreadthFirstTraversalWithPath<T, U, V> traversal,
                                          Predicate<T> defaultPredicate,
                                          U defaultResult, V initialValue);

    /**
     * Allows breadth-first traversing of the network using the visitor passed.
     *
     * @param from Starting node, where the traversal starts.
     * @param traversal Visitor that gets called for each node during traversing.
     * @param defaultPredicate Default predicate to use when choosing which nodes and paths to follow, used if
     *         the call to visitor has not yielded a different Predicate for that path.
     * @param defaultResult Default result to return from the method, if none of the visitor calls yielded a
     *         result.
     * @param initialValue Initial value passed to the nodes in the first round of the breadth-first visitor
     *         calls.
     * @param <U> Result type returned by the visitor and this method.
     * @param <V> Value passed to the visitor for each call.
     * @return The result of the traversal or the defaultResult if none was returned by the visitor.
     */
    <U, V> U traverseBreadthFirst(T from, BreadthFirstTraversal<T, U, V> traversal, Predicate<T> defaultPredicate,
                                  U defaultResult, V initialValue);
}
