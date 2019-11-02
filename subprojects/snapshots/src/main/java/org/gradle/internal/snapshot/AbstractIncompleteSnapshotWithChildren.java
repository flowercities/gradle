/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.internal.snapshot;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractIncompleteSnapshotWithChildren extends AbstractFileSystemNode {
    protected final List<? extends FileSystemNode> children;

    public AbstractIncompleteSnapshotWithChildren(String pathToParent, List<? extends FileSystemNode> children) {
        super(pathToParent);
        this.children = children;
    }

    protected abstract Optional<MetadataSnapshot> getThisSnapshot();

    protected abstract FileSystemNode createCopy(String prefix, List<? extends FileSystemNode> newChildren);
    protected abstract Optional<FileSystemNode> withNoChildren();
    protected abstract FileSystemNode withUnkownChildInvalidated();

    @Override
    public Optional<FileSystemNode> invalidate(String absolutePath, int offset) {
        return SnapshotUtil.handleChildren(children, absolutePath, offset, new SnapshotUtil.ChildHandler<Optional<FileSystemNode>>() {
            @Override
            public Optional<FileSystemNode> handleNewChild(int insertBefore) {
                return Optional.of(withUnkownChildInvalidated());
            }

            @Override
            public Optional<FileSystemNode> handleChildOfExisting(int childIndex) {
                FileSystemNode child = children.get(childIndex);
                return SnapshotUtil.invalidateSingleChild(child, absolutePath, offset)
                    .map(invalidatedChild -> withReplacedChild(childIndex, child, invalidatedChild))
                    .map(Optional::of)
                    .orElseGet(() -> {
                        if (children.size() == 1) {
                            return withNoChildren();
                        }
                        List<FileSystemNode> merged = new ArrayList<>(children);
                        merged.remove(childIndex);
                        return Optional.of(createCopy(getPathToParent(), merged));
                    });
            }
        });
    }

    @Override
    public FileSystemNode update(String absolutePath, int offset, MetadataSnapshot snapshot) {
        return SnapshotUtil.handleChildren(children, absolutePath, offset, new SnapshotUtil.ChildHandler<FileSystemNode>() {
            @Override
            public FileSystemNode handleNewChild(int insertBefore) {
                List<FileSystemNode> newChildren = new ArrayList<>(children);
                newChildren.add(insertBefore, snapshot.withPathToParent(absolutePath.substring(offset)));
                return createCopy(getPathToParent(), newChildren);
            }

            @Override
            public FileSystemNode handleChildOfExisting(int childIndex) {
                FileSystemNode child = children.get(childIndex);
                return withReplacedChild(childIndex, child, SnapshotUtil.updateSingleChild(child, absolutePath, offset, snapshot));
            }
        });
    }

    private FileSystemNode withReplacedChild(int childIndex, FileSystemNode childToReplace, FileSystemNode newChild) {
        if (newChild == childToReplace) {
            return AbstractIncompleteSnapshotWithChildren.this;
        }
        if (children.size() == 1) {
            return createCopy(getPathToParent(), ImmutableList.of(newChild));
        }
        List<FileSystemNode> merged = new ArrayList<>(children);
        merged.set(childIndex, newChild);
        return createCopy(getPathToParent(), merged);
    }

    @Override
    protected Optional<MetadataSnapshot> getChildSnapshot(String absolutePath, int offset) {
        return SnapshotUtil.getSnapshotFromChildren(children, absolutePath, offset);
    }

    @Override
    public FileSystemNode withPathToParent(String newPathToParent) {
        return getPathToParent().equals(newPathToParent)
            ? this
            : createCopy(newPathToParent, children);
    }
}