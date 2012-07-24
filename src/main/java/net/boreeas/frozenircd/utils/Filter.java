/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.boreeas.frozenircd.utils;

/**
 *
 * @author Boreeas
 */
public interface Filter<T> {

    public boolean pass(T instance);
}
