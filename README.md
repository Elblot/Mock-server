# Mock-server

when it run, send it the dot file with a post at the path /rules

The dot sent have to follow some constraints:

-no empty lines

-one transition/ state per line

-the line that describe state or transition have to terminate by a ;  

-the states are define before the transitions

-initial state marked with the transition S00 -> <init>, with S00 define by: S00[shape=point]

-the closing } is on a new line (the last one)

-final states have the shape doublecircle

-other states have the shape circle
