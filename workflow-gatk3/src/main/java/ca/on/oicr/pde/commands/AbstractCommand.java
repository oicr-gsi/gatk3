package ca.on.oicr.pde.commands;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author mlaszloffy
 */
public abstract class AbstractCommand {

    protected final List<String> command = new LinkedList<>();

    public List<String> getCommand() {
        return Collections.unmodifiableList(command);
    }
    
}
