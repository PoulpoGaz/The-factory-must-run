# The factory must run

An unfinished factory game for [OLC CodeJam 2024](https://itch.io/jam/olc-codejam-2024).

## Game description

Manage a factory in this factorio/mindustry inspired game !

Place conveyors, routers and underground conveyors to deliver items from generators to machines and then to consumers. Nevertheless, space is limited and building and producing cost resources. Will you be able to produce an electronic circuit ?


### Game mechanics details:

A conveyor can take items from behind, left or right and output them in front of it. A router take items from behind and output them in front of it or on its right or left. A router can act as a filter and output only in front of him the filtered item. (click on a router to show filter menu). An underground conveyor outputs items to another underground conveyor in a range of 5 according to manhattan distance. You can link/unlink two underground conveyors by clicking on them.<br>
Generators can produce items, select the item by clicking on it. Generators output directly to a conveyor. Machines take items from conveyors which go towards him. A recipe can be selected by opening the machine's sub menu (click on the machine). Machines output items to adjacent conveyors or directly to another machine/consumer. A consumer consume an item and increase your resources.

### Interface details:

In the bottom: blocks than can be placed: in order: conveyor, router, underground conveyor, generator, consumer, machine and wall. The cost of a block is the number on the left
In the top: your resources. You cannot place blocks or produce items who cost more than that. You can see the cost of an item by hovering an item in the generator sub menu.

### Controls:

* LEFT CLICK: select blocks, place blocks, open sub menu for routers, machines and generators, and link/unlink underground conveyors.
* Q: deselect
* RIGTH CLICK: delete block
* R: rotate selected block
* SHIFT+R: swap underground conveyor