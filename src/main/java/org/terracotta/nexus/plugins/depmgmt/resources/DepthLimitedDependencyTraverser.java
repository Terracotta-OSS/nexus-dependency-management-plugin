package org.terracotta.nexus.plugins.depmgmt.resources;

import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencyTraverser;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.util.graph.traverser.StaticDependencyTraverser;

/**
 * A {@link DependencyTraverser} that is limited by the depth it has travelled.
 */
public class DepthLimitedDependencyTraverser implements DependencyTraverser {

  private int depth;
  private int level;

  public DepthLimitedDependencyTraverser(int depth) {
    this(depth, 0);
  }

  DepthLimitedDependencyTraverser(int depth, int level) {
    this.depth = depth;
    this.level = level;
  }

  @Override
  public boolean traverseDependency(Dependency dependency) {
    return level < depth;
  }

  @Override
  public DependencyTraverser deriveChildTraverser(DependencyCollectionContext context) {
    if (level < depth) {
      return new DepthLimitedDependencyTraverser(depth, level + 1);
    } else {
      return new StaticDependencyTraverser(false);
    }
  }
}
