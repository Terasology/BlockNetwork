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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * The result of a node traversal performed on a graph. It contains a flag to stop the traversal, a predicate for selecting consequent 
 * nodes to be traversed, the returned result of the traversal, and the value propagated through the traversal.
 * 
 * @param <T> Type of nodes in the graph.
 * @param <U> Return value type from the traversal.
 * @param <V> Value passed from node visited into nodes that are outgoing from that node.
 */
public final class TraversalResult<T, U, V> {
    // A flag specifying whether to stop the traversal or not.
    public final boolean stopTraversal;
    
    // The predicate that filters or selects the nodes that shall be traversed in the next step.
    public Predicate<T> predicate;
    
    // The result of the node traversal.
    public final U result;
    
    // The value propagated through the node traversal.
    public final V value;

    private TraversalResult(boolean stopTraversal, Predicate<T> predicate, U result, V value) {
        this.stopTraversal = stopTraversal;
        this.predicate = predicate;
        this.result = result;
        this.value = value;
    }

    /**
     * Indicates that you want the traversal to continue along this path (through this node), and you want the specified
     * value to be passed to all nodes that are outgoing from this node. The default Predicate will be used to select
     * nodes to follow through this node.
     * @param value Value to pass to other nodes that are outgoing from this node.
     * @param <T>
     * @param <U>
     * @param <V>
     * @return
     */
    public static <T, U, V> TraversalResult<T, U, V> continuePath(V value) {
        return new TraversalResult<>(false, null, null, value);
    }

    /**
     * Indicates that you want the traversal to continue along this path (through this node), and you want the specified
     * value to be passed to all nodes that are outgoing from this node. The specified Predicate will be used to select
     * nodes to follow through this node.
     * @param value Value to pass to other nodes that are outgoing from this node.
     * @param predicate Predicate used to filter nodes to follow through this node.
     * @param <T>
     * @param <U>
     * @param <V>
     * @return
     */
    public static <T, U, V> TraversalResult<T, U, V> continuePathWithPredicate(V value, Predicate<T> predicate) {
        return new TraversalResult<>(false, predicate, null, value);
    }

    /**
     * Indicates that you don't want to explore nodes that are outgoing from this path.
     * @param <T>
     * @param <U>
     * @param <V>
     * @return
     */
    public static <T, U, V> TraversalResult<T, U, V> skipPath() {
        return new TraversalResult<>(false, Predicates.alwaysFalse(), null, null);
    }

    /**
     * Indicates that you want the traversal to stop and return the specified result.
     * @param result Result to return from the traversal method.
     * @param <T>
     * @param <U>
     * @param <V>
     * @return
     */
    public static <T, U, V> TraversalResult<T, U, V> returnResult(U result) {
        return new TraversalResult<>(true, null, result, null);
    }
}
