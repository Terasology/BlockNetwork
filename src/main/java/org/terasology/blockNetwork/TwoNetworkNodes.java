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
package org.terasology.blockNetwork;

    /**
    * Creates two network nodes, checks whether they are equal to each other or not, and 
    * calculates the sum of the hashcodes of the locations of both nodes.
    */
public class TwoNetworkNodes {
    public final NetworkNode node1;
    public final NetworkNode node2;
    
    /**
    * TwoNetWorkNodes constructor.
    *
    * @param  node1 
    * @param  node2
    */
    public TwoNetworkNodes(NetworkNode node1, NetworkNode node2) {
        this.node1 = node1;
        this.node2 = node2;
    }
    
    /**
    * Checks if two nodes are equal to each other.
    *
    * @param o  an object
    * @return <code>true</code> if node1 and node2 are both equal to each other. Will return <code>false</code> if o 
    * is null, o does not equal the class of this, or node 1 and node 2 are not equal
    */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TwoNetworkNodes that = (TwoNetworkNodes) o;

        return that.node1.equals(node1) && that.node2.equals(node2)
                || that.node1.equals(node2) && that.node2.equals(node1);
    }

    /**
    * Calculates the sum of the hashcodes of node 1 and node2.
    *
    * @return the sum of the hashcodes of the locations of node1 and node2 
    */    
    @Override
    public int hashCode() {
        return node1.hashCode() + node2.hashCode();
    }
}
