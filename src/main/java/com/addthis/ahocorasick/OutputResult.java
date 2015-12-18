/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.ahocorasick;

public class OutputResult {
    /**
     * The payload associated with the located substring.
     */
    private Object output;

    /**
     * The index (included) in the whole string where the located substring
     * starts.
     */
    private int startIndex;

    /**
     * The index (excluded) in the whole string where the located substring
     * ends.
     */
    private int endIndex;

    /**
     * Creates a new OutputResult with the output and the last index passed
     * as parameter. Set the startIndex attribute only if the output object
     * is a String.
     */
    OutputResult(Object output, int startIndex, int endIndex) {
        this.output = output;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    /**
     * @return output associated with the match
     */
    public Object getOutput() {
        return output;
    }

    /**
     * @return start position associated with the match
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return end position associated with the match
     */
    public int getEndIndex() {
        return endIndex;
    }

    /**
     * An output with span <code>(start1,last1)</code> overlaps an output with
     * span <code>(start2,last2)</code> if and only if either end points of the
     * second output lie within the first chunk:<code>start1 &lt;= start2 &lt; last1</code>, or
     * <code>start1 &lt; last2 &lt;= last1</code>.
     */
    boolean isOverlapped(OutputResult other) {
        return (this.startIndex <= other.startIndex && other.startIndex < this.endIndex)
               || (this.startIndex < other.endIndex && other.endIndex <= this.endIndex);
    }

    /**
     * An output <code>output1=(start1,last1)</code> dominates another output
     * <code>output2=(start2,last2)</code> if and only if the outputs overlap
     * and: <code>start1 &lt; start2</code> (leftmost), or
     * <code>start1 == start2</code> and <code>last1 &gt; last2</code>
     * (longest).
     */
    boolean dominate(OutputResult other) {
        return isOverlapped(other)
               && ((this.startIndex < other.startIndex) ||
                   (this.startIndex == other.startIndex && this.endIndex > other.endIndex));
    }

    @Override
    public String toString() {
        return "[" + getStartIndex() + "," + getEndIndex() + "]: " + getOutput();
    }
}
