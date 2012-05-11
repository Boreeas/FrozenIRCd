/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.boreeas.frozenircd.connection;

/**
 *
 * @author Boreeas
 */
public interface BroadcastFilter {

    public boolean sendToConnection(Connection connection);
}
