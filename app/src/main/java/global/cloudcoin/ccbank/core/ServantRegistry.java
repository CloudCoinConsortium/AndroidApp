package global.cloudcoin.ccbank.core;


import java.util.Hashtable;
import java.lang.reflect.Constructor;

import javax.microedition.khronos.opengles.GL;

import global.cloudcoin.ccbank.core.GLogger;




public class ServantRegistry {

    private String ltag = "ServantRegistry";

    Hashtable<String, Servant> servants;

    public ServantRegistry() {
        this.servants = new Hashtable<String, Servant>();
    }

    private void setServant(String name, Object servant) {
        this.servants.put(name, (Servant) servant);
    }

    public Servant getServant(String name) {
        return (Servant) this.servants.get(name);
    }

    public boolean registerServants(String[] servants, String rootDir, GLogger logger) {
        for (String servant : servants) {
            if (!registerServant(servant, rootDir, logger)) {
                logger.error(ltag, "Failed to register servant " + servant);
                return false;
            }
        }

        return true;
    }

    public boolean registerServant(String name, String rootDir, GLogger logger) {
        Package packageObj = this.getClass().getPackage();
        String classBinName = packageObj.getName();


        classBinName = classBinName.substring(0, classBinName.lastIndexOf('.'));

        try {
            ClassLoader classLoader = this.getClass().getClassLoader();

            classBinName += "." + name + "." + name;

            // Load the target class using its binary name
            Class loadedMyClass = classLoader.loadClass(classBinName);


            Constructor constructor = loadedMyClass.getDeclaredConstructor(String.class, GLogger.class);
            Object myClassObject = constructor.newInstance(rootDir, logger);

            setServant(name, myClassObject);
        } catch (ClassNotFoundException e) {
            logger.error(ltag, "Class not found for " + name + " " + classBinName);
            e.printStackTrace();

            return false;
        } catch (Exception e) {
            logger.error(ltag, "Failed to instantiate class");
            e.printStackTrace();

            return false;
        }

        return true;
    }


}
