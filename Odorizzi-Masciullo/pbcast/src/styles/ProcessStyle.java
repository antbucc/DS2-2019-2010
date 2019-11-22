package styles;

import java.awt.Color;

import repast.simphony.visualizationOGL2D.DefaultStyleOGL2D;
import pbcast.Process;
import pbcast.Process.ProcessType;

public class ProcessStyle extends DefaultStyleOGL2D {

    @Override
    public Color getColor(Object object) {
        Process a;
        if (object instanceof Process) {
            a = (Process) object;
            if (a.getCurrentType() == ProcessType.QUIETE)
            	return Color.BLUE;
            else if (a.getCurrentType() == ProcessType.SENDER)
            	return Color.RED;
            else if (a.getCurrentType() == ProcessType.RECIEVER)
            	return Color.GREEN;
            else if (a.getCurrentType() == ProcessType.CHILD)
            	return Color.YELLOW;
        }
        return Color.BLACK;
    }
}
