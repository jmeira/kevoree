package org.kevoree.library.kotlin;

import org.kevoree.annotation.ComponentType;
import org.kevoree.annotation.Start;
import org.kevoree.annotation.Stop;
import org.kevoree.framework.AbstractComponentType;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 11/05/12
 * Time: 17:42
 */

@ComponentType
public class KotlinHelloWorld extends AbstractComponentType {

    public static String JavaHelloString = "Hello from Java!";
    /*
    public static String getHelloStringFromKotlin() {
        return namespace.KotlinHelloString;
    }

    private KotlinHelper khel = new KotlinHelper();
   */
    @Start
    public void start() {

        MyTest t = new MyTest();
        t.titi("RR");

    }

    @Stop
    public void stop() {

    }

}
