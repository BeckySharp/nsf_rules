import networkx as nx
from matplotlib import pyplot as plt
from IPython.display import Image, display
import matplotlib.pyplot as plt
import numpy as np 
import random 
import json
import os, sys


class Rule():
    def __init__(self, steps, directions, edge_labels, node_attributes):
        self.steps = steps
        self.directions = directions
        self.edge_labels = edge_labels
        self.node_attributes = node_attributes

class Vocabulary():
    def __init__(self, filename):
        self.idx2label = list(self.load_vocabulary(filename))
        self.label2id = {}
        for i, label in enumerate(self.idx2label):
            self.label2id[label] = i
        self.size = len(self.idx2label)

    def load_vocabulary(self, fn):
        with open(fn) as f:
            for line in f:
                line = line.strip()
                yield line

    def node_color(self, n):
        color = ("white", "white")
        if n % 3 == 0:
            color = ("cornflowerblue", "blue")
        if n % 4 == 0:
            color = ("coral", "orange")
        if n % 5 == 0:
            color = ("gray87", "gray")
        return color

class LabeledGraph():
    '''
    types :
    - edges: List of (u, v) tuples from mk labled graph
    - label: String, the label on all edges contained in this object/subgraph
    '''
    def __init__(self, edges, label):
        self.graph = nx.DiGraph()
        self.graph.add_edges_from(edges)
        self.matrix = nx.to_numpy_matrix(self.graph).astype(np.int32)
        self.transpose = self.matrix.transpose()
        self.label = label
        self.edges = edges 

    def get_row(self, node, direction):
        if direction == 'OUT': 
            if len(self.matrix) > node:
                return self.matrix[node] # node current stanting from zero to how many nodes one of the circles returns the rows of places 
        else:
            if len(self.transpose) > node:
                return self.transpose[node]
        return np.empty(0)


# Make a LabeledGraph instance.
# Options:
#   1. take a random walk over the nodes in the source_graph
#   2. randomly choose edges and add them, meaning that the "steps" don't
#      need to be connected to each other
# TODO: I think we should start with the second option...!

def mk_labeled_graph(source_graph, label, num_steps):
    A = nx.to_numpy_matrix(source_graph)
    edges = []
    step = 0
    while step < num_steps:
        start = random.randint(0, len(A))
        end = random.randint(0, A.shape[1])
        x = (start, end)
        edges.append(x)
        step += 1
    return LabeledGraph(edges, label)# row of object 

# Make a random walk over the collection of LabeledGraphs
#   1. choose starting position
#   2. choose direction (incoming/outgoing)
#   3. check to see which subgraphs have options for current node and selected direction
#   4. randomly choose from the subgraphs available from (3)
#   5. from that subgraph, given the selected direction, randomly choose the next node
#   6. continue until (a) max steps reached or (b) nowhere to go
#   7. return either a walk or None
def random_walk_over_labeled_edges(subgraphs_for_edge_types, num_steps, source_graph):
    edge_labels = []  
    tuple_steps = [] 
    start = choose_start(source_graph)
    directions = []
    node_attributes = []
    # attribute for the start node
    node_attributes.append(choose_attribute())
    curr = start
    steps_taken = 0
    while steps_taken < num_steps:
        direction = choose_direction()
        available_subgraphs = check_subgraphs(subgraphs_for_edge_types, curr, direction) #instances that qualify; if not any then ? break ?
        if len(available_subgraphs) == 0:
            break
        selected_subgraph = choose_subgraph(available_subgraphs)
        edge_labels.append(selected_subgraph.label)  # a labeled instance  
        row = selected_subgraph.get_row(curr, direction)
        next_node = choose_from_row(row) # poss next check ??
        if next_node == None: # 0.0 ? 
            break
        tuple_steps.append((int(curr), int(next_node))) # tuple needded to create a graph 
        directions.append(direction)
        node_attributes.append(choose_attribute())
        curr = next_node
        steps_taken += 1

    if steps_taken >= num_steps:
        # list of nodes visted(edges), direction In or out, labels traversed, and the attributes by which to
        # identify the nodes
        return Rule(tuple_steps, directions, edge_labels, node_attributes)

    else:
        return None

def choose_start(source_graph): # no parameter we need the matrix/ the length of the list of edged tuples ? 
    A = nx.to_numpy_matrix(source_graph) # Just have the one function of the matrix of source graph ? 
    return random.randint(0, len(A))

# randomly choose a direction (incoming vs outgoing)
def choose_direction():
    return random.choice(["IN", "OUT"])

def choose_attribute():
    # weights, to choose name more often
    return random.choice(["NAME"]*6 + ["COLOR"]*4)
   

# return the graphs that have available transitions for that node and direction
def check_subgraphs(subgraphs_for_edge_types, node, direction): #?
    '''
    out side flips and list labeled edge objects 
    - check if can I traverse does it have out , in of or in the 
    return a list of indices of the posible list of subtypes 
    - diff each suubgraph 
    '''
    poss_edge_types = []
    for i, Labeledgraphinstance in  enumerate(subgraphs_for_edge_types):
        if 1.0 in Labeledgraphinstance.get_row(node, direction):
            poss_edge_types.append(subgraphs_for_edge_types[i]) # i or labeled graph instance ? mistake ???
    return poss_edge_types


# Randomly select a subgraph from the list and return it
def choose_subgraph(available_subgraphs): # list of objects 
   '''
   randomly chooseing from a list of objects: avalible_subgraphs
   and returns an  instance 
   '''
   return  random.choice(available_subgraphs)

# Randomly select a next node from the available transitions in the row
def choose_from_row(row):
    '''
    parameter: list of possible rows from a labled graph matrix transposition
    select the next possible row 
    '''
    row  = np.squeeze(np.asarray(row))
    one = np.where(row >0)[0] # returns as tuple take from return # retunrns 0 is places where there grt than zero.
    return random.choice(one)


# Randomly choose a number of steps between the min and max
def random_between(min, max):
    return random.randint(min, max)


def save(stimuli_list): # NEED HELP ON 
    '''
    stimuli_list save as a json lines text file
    '''
    with open('stimuli.jsonl', 'w') as file:
        for i in stimuli_list:
            # dump each individual stimulus as its own json object,
            # one per line
            json.dump(i.__dict__, file)
            file.write("\n")

def visualize(outdir, id, subgraph_list, node_vocabulary):
    '''
    Save a png of the overall graph against which the user will try to match the rule/pattern
    '''
    merged = nx.MultiDiGraph()
    for g in  subgraph_list:
        edges = set(g.edges) # deduplicate
        label = g.label
        for (u, v) in edges:
            if not merged.has_node(u):
                merged.add_node(
                    u,
                    label=node_vocabulary.idx2label[u],
                    style="filled",
                    fillcolor=node_vocabulary.node_color(u)[0]
                )
            if not merged.has_node(v):
                merged.add_node(
                    v,
                    label=node_vocabulary.idx2label[v],
                    style="filled",
                    fillcolor=node_vocabulary.node_color(v)[0]
                )
            merged.add_edge(u,v, label = label)
    p = nx.drawing.nx_pydot.to_pydot(merged) # The same or a new file each time ?

    outfile = os.path.join(outdir, f"full_graph_{id}.png")
    p.write_png(outfile)
    Image(filename=outfile)


def main():

    # ----------------------------------
    # Step 0: Initialize everything
    # ----------------------------------

    outdir = "."


    # -- Vocabularies --
    num_nodes = 8
    node_vocab = Vocabulary("node_vocabulary.txt")
    node_indices = list(range(node_vocab.size))
    np.random.shuffle(node_indices)
    node_sample = node_indices[:num_nodes]

    num_edges = 6
    edge_vocab = Vocabulary("edge_vocabulary.txt")
    edge_indices = list(range(edge_vocab.size))
    np.random.shuffle(edge_indices)
    edge_sample = edge_indices[:num_edges]



    # -- Params --
    num_stimuli = 20
    # these are for controlling the number of edges that can occur
    # for each of the edge labels.  The resulting subgraphs, together,
    # when **visualized** will be the GRAPH shown at the top of a page
    # in the survey.
    min_steps_for_subgraph = 3
    max_steps_for_subgraph = 10

    # These are for controlling how long the actual rule is for the stimuli_rules.
    min_stimuli_walk_length = 4
    max_stimuli_walk_length = 10

    # ----------------------------------
    # Step 1: generate a graph from which we will get the subgraphs
    # ----------------------------------

    source_graph = nx.complete_graph(num_nodes)

    # ----------------------------------
    #  Step 2: for each edge label in the vocabulary, make a LabeledGraph
    # ----------------------------------

    subgraphs_for_edge_types = []
    for i, edge_label_idx in enumerate(edge_indices):
        label = edge_vocab.idx2label[edge_label_idx]
        num_steps = random_between(min_steps_for_subgraph, max_steps_for_subgraph)
        subgraph = mk_labeled_graph(source_graph, label, num_steps) # a LabledGraph instance
        subgraphs_for_edge_types.append(subgraph) # list of labeled graphs instances  
    # ----------------------------------
    # Step 3: for 1..N (num stimuli rules), generate and store random walks
    #           over those LabeledGraphs
    # ----------------------------------

    stimuli_rules = []
    while len(stimuli_rules) < num_stimuli:
        num_steps = random_between(min_stimuli_walk_length, max_stimuli_walk_length)
        current_walk = random_walk_over_labeled_edges(subgraphs_for_edge_types, num_steps, source_graph)
        if current_walk != None:
            stimuli_rules.append(current_walk)
    # ----------------------------------
    # Step 4: Export the materials for Qualtrics
    # ----------------------------------

    # save and/or visualize stimuli_rules
    save(stimuli_rules)
    # todo: export rules in format we can calulate coverage of the factors of interest

    # visualize the full "graph" with the labeled edges, will be at the top of the
    # page in the survey
    visualize(outdir, 1, subgraphs_for_edge_types, node_vocab)

    # todo: export full graph in format we can calulate coverage of the factors of interest



if __name__ == '__main__':
    main()