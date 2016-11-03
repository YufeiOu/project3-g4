This is the directory containing strategy files that we designed to project 3 of the Programming & Problem Solving course http://www.cs.columbia.edu/~kar/4444f16/

Notes are kept here: https://docs.google.com/document/d/1wV54gxhkbN5TTwwFcgW7zFeqD0Q8af-nHMhj3pZVqO4/edit

The files themselves are not meaningful unless they are compiled together with the simulator, for those who have the simulator and want to change/add files under this directory, please keep in mind we should rename this "project3-g4" to "g4" under your local directory ./sqdance/

Tips to help you run/compile the code:

To run or compile the simulator, cd into the folder above sqdance

To (re)compile the simulator on Unix & Mac OS X:   

    $ javac sqdance/sim/*.java

To (re)compile the simulator on Windows:          

    $ javac sqdance\sim\*.java

To run the simulator:  

    $ java sqdance.sim.Simulator <arguments>
    
The simulator arguments are:

    -g <group name, e.g. g4>
    //TODO: add information here when simulator instruction is ready
    --gui

For example, we should usually run like this: 

    $ java sqdance.sim.Simulator --gui -g g4
