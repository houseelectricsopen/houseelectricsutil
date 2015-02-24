package com.houseelectrics.util;

import java.lang.reflect.*;
import java.util.*;

/**
 * Created by roberttodd on 02/12/2014.
 */
public class ReflectionUtil
{
    public static interface ObjectCreator
    {
        public Object newInstance(Object parentContext, String propertyName, Class theClass);
    }

    public static class PropertyReference
    {
        private Class parentType;
        public Class getParentType() {return parentType;}
        private Object parent;
        public Object getParent() {return parent;}
        public void setParent(Object parent) {this.parent=parent;}
        private String propertyName;
        public String getPropertyName() {return propertyName;}
        public void setPropertyName(String propertyName) {this.propertyName = propertyName;}
        private Class type;
        public Class getType() {return type;}
        public void setType(Class value) {this.type=value;}
        private Type[] parametrisedTypes;
        public void setParametrisedTypes(Type[] value) {this.parametrisedTypes=value;}
        public Type[] getParametrisedTypes() {return parametrisedTypes;}

        private Method setterMethod;
        public Method getSetterMethod() {return setterMethod;}

        private Method getterMethod;
        public Method getGetterMethod() {return getterMethod;}

        public void set(Object value) throws IllegalAccessException, InvocationTargetException
        {
            Object []args = {value};
            setterMethod.invoke(parent, args);
        }
        public Object get()  throws IllegalAccessException, InvocationTargetException
        {
            Object []args = {};
            return getterMethod.invoke(parent, args);
        }

        public void set(Object parent, Object value) throws IllegalAccessException, InvocationTargetException
        {
            Object []args = {value};
            setterMethod.invoke(parent, args);
        }
        public Object get(Object parent)  throws IllegalAccessException, InvocationTargetException
        {
            Object []args = {};
            return getterMethod.invoke(parent, args);
        }

    }

    final static Class[] noargs = {};

    public static Method getMethodForPropertyName(Class theClass, String propertyName) throws NoSuchMethodException
    {
        String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) +
                propertyName.substring(1);
        Method m = theClass.getMethod(methodName, noargs);
        return m;
    }

    public static Method getSetterMethodForPropertyName(Class theClass, String propertyName) throws NoSuchMethodException
    {
        String methodName = "set" + Character.toUpperCase(propertyName.charAt(0)) +
                propertyName.substring(1);
        for (Method m : theClass.getMethods())
        {
            if (m.getName().equals(methodName) && m.getParameterTypes().length==1)
            {
                return m;
            }
        }

        return null;
    }


    public static Object getPropertyValueReturnExceptions(Object o, String propertyName)
    {
        return getPropertyValueReturnExceptions(o, propertyName, null);
    }

    public static Object getPropertyValueReturnExceptions(Object o, String propertyName, ObjectCreator autocreate)
    {
        try {
            Method m = getMethodForPropertyName(o.getClass(), propertyName);
            Object result = m.invoke(o, noargs);
            if (result==null && autocreate!=null)
            {
                result = autocreate.newInstance(o, propertyName, m.getReturnType());
                Method mSetter = getSetterMethodForPropertyName(o.getClass(), propertyName);
                Object args[] = {result};
                mSetter.invoke(o, args);
            }
            return result;
        }
        catch (Exception ex)
        {
            return ex;
        }

    }
    public static Object getFieldValueReturnExceptions(Object o, String fieldName)
    {
        try {
            Field f = o.getClass().getField(fieldName);
            return f.get(o);
        }
        catch (Exception ex)
        {
            return ex;
        }

    }

    public static Type[] getParameterizedTypesForField(Class parentClass, String fieldName)
    {
        try
        {
            Field thefield = parentClass.getDeclaredField(fieldName);
            Type  genericType = thefield.getGenericType();
            if (!(genericType instanceof ParameterizedType))
            {
                return null;
            }
            ParameterizedType pType = (ParameterizedType)genericType ;
            return pType.getActualTypeArguments();
        }
        catch (NoSuchFieldException nex)
        {
            return null;
        }
    }

    public static Type[] getParameterizedTypesForPropertyByGetter(Method getter)
    {
            Type  genericType = getter.getGenericReturnType();
            if (!(genericType instanceof ParameterizedType))
            {
                return null;
            }
            ParameterizedType pt = (ParameterizedType) genericType;
            return pt.getActualTypeArguments();
    }

    public static Type[] getParameterizedTypesForProperty(Class parentClass, String fieldName)
    {
        try
        {
            Method getter = getMethodForPropertyName(parentClass, fieldName);
            return getParameterizedTypesForPropertyByGetter(getter);
        }
        catch (NoSuchMethodException nex)
        {
            return null;
        }
    }

    public static Class getPropertyOrReflectionType(Class theClass, String propertyName)
    {
        Method m = null;
        Class result=null;
        try
        {
            m= ReflectionUtil.getMethodForPropertyName(theClass, propertyName);
        } catch (Exception ex)
        {

        }
        if (m != null)
        {
            result = m.getReturnType();
        } else
        {
            try
            {
                Field f = theClass.getField(propertyName);
                result = f.getType();
            }
            catch (NoSuchFieldException nex)
            {

            }
        }
        return result;
    }




    public static Object valueForKeyPathWithIndexes(Object source, String path, boolean returnNull4OutOfRangeIndex,
                                                    boolean throwExceptionOnError)
    {
        return valueForKeyPathWithIndexes(source,  path,  returnNull4OutOfRangeIndex, throwExceptionOnError, null);
    }


    public static PropertyReference getPropertyReferenceForKeyPathWithIndexes(Object root, String path, boolean returnNull4OutOfRangeIndex,
                                                                           boolean throwExceptionOnError, ObjectCreator autoCreate)
            throws NoSuchMethodException
    {
        int lastDotIndex = path.lastIndexOf(".");
        Object parent;
        String propertyName;
        if (lastDotIndex != -1)
        {
            String parentPath = path.substring(0, lastDotIndex);
            parent = valueForKeyPathWithIndexes(root, parentPath, returnNull4OutOfRangeIndex, throwExceptionOnError, autoCreate);

            propertyName = path.substring(lastDotIndex+1);
        }
        else
        {
            parent = root;
            propertyName = path;
        }
        PropertyReference reference = new PropertyReference();
        reference.parentType = parent.getClass();
        reference.setPropertyName(propertyName);
        reference.setterMethod = getSetterMethodForPropertyName(parent.getClass(), propertyName);
        reference.getterMethod = getMethodForPropertyName(parent.getClass(), propertyName);
        reference.parent = parent;
        reference.type = reference.getterMethod.getReturnType();
        reference.setParametrisedTypes(getParameterizedTypesForPropertyByGetter(reference.getterMethod));
        return reference;

    }


    public static List<PropertyReference> getPublicReadWriteableProperties(Class theClass)
    {
        Map<String, Method> propertyNames2SetMethods = new HashMap<String, Method>();
        Map<String, Method> propertyNames2GetMethods = new HashMap<String, Method>();
        for  (Method m : theClass.getMethods())
        {
            if (!Modifier.isPublic(m.getModifiers())) continue;
            if (m.getDeclaringClass() == Object.class) continue;
            Class[] prms = m.getParameterTypes();
            if (m.getName().startsWith("get") && prms.length == 0 && m.getReturnType() != null)
            {
                propertyNames2GetMethods.put(m.getName().substring(3), m);
            } else if (m.getName().startsWith("set") && prms.length == 1 && (m.getReturnType() == Void.TYPE) )
            {
                propertyNames2SetMethods.put(m.getName().substring(3), m);
            }
        }
        List<PropertyReference> references = new ArrayList<PropertyReference>();
        for (String propertyName : propertyNames2GetMethods.keySet())
        {
            if (!propertyNames2SetMethods.containsKey(propertyName)) continue;
            PropertyReference pr = new PropertyReference();
            pr.setterMethod = propertyNames2SetMethods.get(propertyName);
            pr.getterMethod = propertyNames2GetMethods.get(propertyName);
            pr.type = pr.getterMethod.getReturnType();
            pr.propertyName = propertyName;
            pr.parentType = theClass;
            references.add(pr);
        }
        return references;

    }


    public static Object valueForKeyPathWithIndexes(Object source, String path, boolean returnNull4OutOfRangeIndex,
                                    boolean throwExceptionOnError, ObjectCreator autoCreate)

    {
        if (path.length()==0) {return source;}
        String[] elements = path.split("\\.");
        Object context = source;
        for (int done=0; done < elements.length; done++)
        {
            String element = elements[done].trim();
            Integer index = null;
            if (element.contains("["))
            {
                int indexBracketIndex = element.indexOf('[');
                String strIndex = element.substring(indexBracketIndex+1, element.length()-1);
                index = Integer.parseInt(strIndex);
                element = element.substring(0, indexBracketIndex);
            }
            element = element.substring(0,1).toUpperCase() + element.substring(1);
            Object parent = context;
            if (element.equals("Count") && context instanceof List)
            {
                context = ((List)context).size();
            }
            else
            {
                //boolean isAutocreate = done<(elements.length-1);
                context = ReflectionUtil.getPropertyValueReturnExceptions(context, element, autoCreate );

                if (index!=null)
                {
                    List lcontext = (List)context;
                    if ((lcontext== null && returnNull4OutOfRangeIndex)  || index>=lcontext.size()) context = null;
                    else context = lcontext.get(index);
                }
            }
            if (context instanceof Throwable)
            {
                if (throwExceptionOnError)
                {
                    String message = "failed to extract " + parent.getClass().getName() + "." + element;
                    RuntimeException rex = new RuntimeException(message, (Exception) context);
                    throw rex;
                }
                else
                {
                    context = null;
                }
            }
            if (context==null) break;
        }

        return context;
    }

public interface DeepComparePropertyReferenceDetector
{
    public boolean isReference(Class type/*, Type[] genericTypeParameters*/);
}

public interface DeepCompareListener
{
    public void differenceDetected(Object o1, Object o2, Stack<String> parentNameStack, String description);
}

    public static class DeepCompareDifference
    {
        public Object o1;
        public Object o2;
        public String propertyPath;
        public String description;
    }


    public static List<DeepCompareDifference> deepCompareViaReadWriteableProperties(Object o1, Object o2) throws Exception
{
    final List<DeepCompareDifference> diffsDetected = new ArrayList<DeepCompareDifference>();
    DeepCompareListener deepCompareListener = new DeepCompareListener()
    {
        @Override
        public void differenceDetected(Object o1, Object o2, Stack<String> parentNameStack, String description)
        {
            DeepCompareDifference diff = new DeepCompareDifference();
            diff.o1 = o1;
            diff.o2 = o2;
            StringBuffer sb = new StringBuffer();
            for (int done=0; done<parentNameStack.size(); sb.append(done==0?"":"."), sb.append(parentNameStack.get(done)), done++ ) {}
            diff.propertyPath = sb.toString();
            diff.description = description;
            diffsDetected.add(diff);
        }
    };
    deepCompareViaReadWriteableProperties( o1,  o2, deepCompareListener);
   return diffsDetected;
}

    public final static DeepComparePropertyReferenceDetector defaultDeepComparePropertyReferenceDetector =
    new DeepComparePropertyReferenceDetector()
    {
      @Override
    public boolean isReference(Class type)
    {
        return !type.isPrimitive() && !Number.class.isAssignableFrom(type) && type!=String.class && type!=Boolean.class
                && type!=Character.class && type!=Byte.class;
    }
};
    public static boolean deepCompareViaReadWriteableProperties(Object o1, Object o2,
                                                                DeepCompareListener deepComparisonListener) throws Exception
    {
        DeepComparePropertyReferenceDetector propertyReferenceDetector = defaultDeepComparePropertyReferenceDetector;

        return deepCompareViaReadWriteableProperties( o1,  o2,
                 null,  propertyReferenceDetector,
                 deepComparisonListener);

    }
//TODO check for loops
private static boolean deepCompareViaReadWriteableProperties(Object o1, Object o2,
                                                            Stack<String> parentNameStack, DeepComparePropertyReferenceDetector propertyReferenceDetector,
                                                            DeepCompareListener deepComparisonListener)
        throws Exception
{
    if (parentNameStack==null) {         parentNameStack = new Stack<String>();   }
    if (o1==o2) return true;
    if (o1==null && o2!=null || o1!=null && o2==null )
        {
            deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "different values:" + o1 +"," + o2);
            return false;
        }
    if (o1.getClass()!=o2.getClass())
    {
        deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "different types:" + o1.getClass().getName() + "," + o2.getClass().getName());
        return false;
    }

    boolean overallMatch = true;
    boolean isList = List.class.isAssignableFrom(o1.getClass());
    boolean isMap = Map.class.isAssignableFrom(o1.getClass());
    boolean isReference = propertyReferenceDetector.isReference(o1.getClass());

    if (isMap)
    {
        //deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "map types always assumed to be different");
        Map map1 = (Map) o1;
        Map map2 = (Map) o2;
        for (Object key : map1.keySet())
        {
            parentNameStack.push("" + key);
            if (!map2.containsKey(key))
            {
                deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "map key not present in right side ");
                overallMatch=false;
            }
            else
            {
                Object mapValue1 = map1.get(key);
                Object mapValue2 = map2.get(key);
                boolean match = deepCompareViaReadWriteableProperties(mapValue1, mapValue2,  parentNameStack, propertyReferenceDetector,deepComparisonListener);
                if (!match) overallMatch=false;
            }
            parentNameStack.pop();
        }
        for (Object key : map2.keySet())
        {
            parentNameStack.push("" + key);
            if (!map1.containsKey(key))
            {
                deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "map key not present in left side ");
                overallMatch=false;
            }
            parentNameStack.pop();
        }
        return overallMatch;
    }
    else if (isList)
    {
        List list1 = (List)o1;
        List list2 = (List)o2;
        if (list1.size()!=list2.size())
        {
            deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "different list sizes " + list1.size() + "," + list2.size());
            return false;
        }
        for (int done=0; done<list1.size(); done++)
        {
            parentNameStack.push(""+done);
            boolean match = deepCompareViaReadWriteableProperties(list1.get(done), list2.get(done),  parentNameStack, propertyReferenceDetector,deepComparisonListener);
            parentNameStack.pop();
            if (!match) overallMatch=false;
        }
        return overallMatch;
    }
    else if (!isReference)
    {
        overallMatch = o1.equals(o2);
        if (!overallMatch)
        {
            deepComparisonListener.differenceDetected(o1, o2, parentNameStack, "different values " + o1 + "," + o2);
        }
    }
    else
    {
        List<PropertyReference> propertyReferences = getPublicReadWriteableProperties(o1.getClass());
        for (int done = 0; done < propertyReferences.size(); done++)
        {
            PropertyReference propertyReference = propertyReferences.get(done);
            Object sub1 = propertyReference.get(o1);
            Object sub2 = propertyReference.get(o2);
            boolean match;
            parentNameStack.push(propertyReference.getPropertyName());
            match = deepCompareViaReadWriteableProperties(sub1, sub2,  parentNameStack,propertyReferenceDetector, deepComparisonListener);
            if (!match)
            {
                overallMatch = false;
            }
            parentNameStack.pop();
        }
    }

    return overallMatch;

}
    public static Object deepCloneViaReadWriteableProperties(Object o1)
            throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        return deepCloneViaReadWriteableProperties(o1, defaultDeepComparePropertyReferenceDetector);
    }


    //the second parameter should maybe be a test for finality ?
    public static Object deepCloneViaReadWriteableProperties(Object o1, DeepComparePropertyReferenceDetector deepComparePropertyReferenceDetector)
            throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        if (o1==null  )
        {
            return null;
        }
        boolean isList = List.class.isAssignableFrom(o1.getClass());
        boolean isMap = Map.class.isAssignableFrom(o1.getClass());
        boolean isReference = defaultDeepComparePropertyReferenceDetector.isReference(o1.getClass());

        Object result;
        if (isMap)
        {
            //deepComparisonListener.differenceDetected (o1, o2, parentNameStack, "map types always assumed to be different");
            Map map1 = (Map) o1;
            Map map2 = (Map) o1.getClass().newInstance();
            for (Object key : map1.keySet())
            {
                Object mapItem = map1.get(key);
                // assume the keys are immutable !
                mapItem = deepCloneViaReadWriteableProperties(mapItem, deepComparePropertyReferenceDetector);
                map2.put(key, mapItem);
            }
            result = map2;
        }
        else if (isList)
        {
            List list1 = (List)o1;
            List list2 = (List) o1.getClass().newInstance();
            for (int done=0; done<list1.size(); done++)
                {
                    Object listItem = list1.get(done);
                    listItem = deepCloneViaReadWriteableProperties(listItem, deepComparePropertyReferenceDetector);
                    list2.add(listItem);
                }
            result =  list2;
        }
        else if (!isReference)
        {
            result = o1;
        }
        else
        {
            result = o1.getClass().newInstance();
            List<PropertyReference> propertyReferences = getPublicReadWriteableProperties(o1.getClass());
            for (int done = 0; done < propertyReferences.size(); done++)
            {
                PropertyReference propertyReference = propertyReferences.get(done);
                Object subObject = propertyReference.get(o1);
                subObject = deepCloneViaReadWriteableProperties(subObject, deepComparePropertyReferenceDetector);
                propertyReference.set(result, subObject);
            }
        }
        return result;
    }

}
