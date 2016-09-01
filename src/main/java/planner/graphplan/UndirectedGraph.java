package planner.graphplan;

import com.google.common.collect.Multimap;

import java.util.Set;

public class UndirectedGraph<T> {
    private Multimap<T, T> graph;

    public UndirectedGraph(Multimap<T, T> graph) {
        this.graph = graph;
    }

    public void addEdge(T e1, T e2) {

    }

    public Set<T> getRelations(T e) {
        return null;
    }
}
