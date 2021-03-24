/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sockmower.misguidedmod.repack.io.airlift.compress.zstd;

class HuffmanTableWriterWorkspace
{
    // for encoding weights
    public final byte[] weights = new byte[Huffman.MAX_SYMBOL]; // the weight for the last symbol is implicit

    // for compressing weights
    public final int[] counts = new int[Huffman.MAX_TABLE_LOG + 1];
    public final short[] normalizedCounts = new short[Huffman.MAX_TABLE_LOG + 1];
    public final FseCompressionTable fseTable = new FseCompressionTable(Huffman.MAX_FSE_TABLE_LOG, Huffman.MAX_TABLE_LOG);
}
