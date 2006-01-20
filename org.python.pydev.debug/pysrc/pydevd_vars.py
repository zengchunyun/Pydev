""" pydevd_vars deals with variables:
    resolution/conversion to XML.
"""
from types import *
import urllib
import threading
import sys
import pydevd_resolver
import traceback

try:
    __setFalse = False
except:
    #early versions of python do not have it.
    False = 0
    True = 1

class VariableError(Exception):
    def __init__(self, message):
        Exception.__init__(self, message)
        
typeMap = {}
try:
    #jython does not have this types
   try:
       typeMap[BooleanType] = (BooleanType, BooleanType.__name__, None)
   except NameError:
       pass #early versions of python do not have it.
   typeMap[BufferType] = (BufferType, BufferType.__name__, None)   
   typeMap = {
       NoneType : (NoneType, NoneType.__name__, None),
       IntType : (IntType, IntType.__name__, None),
       LongType : (LongType, LongType.__name__, None),
       FloatType : (FloatType, FloatType.__name__, None),
       ComplexType : (ComplexType, ComplexType.__name__, None),
       StringType : (StringType, StringType.__name__, None),
       UnicodeType : (UnicodeType, UnicodeType.__name__, None),
       TupleType : (TupleType, TupleType.__name__, pydevd_resolver.tupleResolver),
       ListType : (ListType, ListType.__name__, pydevd_resolver.tupleResolver),
       DictType : (DictType, DictType.__name__, pydevd_resolver.dictResolver)
   }
except:   
   from org.python import core
   typeMap = {
       core.PyNone : ( core.PyNone, core.PyNone.__name__, None),
       core.PyInteger : ( core.PyInteger, core.PyInteger.__name__, None),
       core.PyLong : ( core.PyLong, core.PyLong.__name__, None),
       core.PyFloat : ( core.PyFloat, core.PyFloat.__name__, None),
       core.PyComplex : ( core.PyComplex, core.PyComplex.__name__, None),
       core.PyString : ( core.PyString, core.PyString.__name__, None),       
       core.PyTuple : ( core.PyTuple, core.PyTuple.__name__, pydevd_resolver.tupleResolver),
       core.PyList : ( core.PyList, core.PyList.__name__, pydevd_resolver.tupleResolver),
       core.PyDictionary: (core.PyDictionary, core.PyDictionary.__name__, pydevd_resolver.dictResolver),
       core.PyJavaInstance: (core.PyJavaInstance, core.PyJavaInstance.__name__, pydevd_resolver.instanceResolver),
       core.PyStringMap: (core.PyStringMap, core.PyStringMap.__name__, pydevd_resolver.dictResolver)       
   }   
   pass


def getType(o):
    """ returns a triple (typeObject, typeString, resolver
        resolver != None means that variable is a container, 
        and should be displayed as a hierarchy.
        Use the resolver to get its attributes.
        
        All container objects should have a resolver.
    """    
    
    try:        
        if type(o).__name__=='org.python.core.PyJavaInstance':
            return (type(o), type(o).__name__, pydevd_resolver.instanceResolver)
        
        if type(o).__name__=='org.python.core.PyArray':
            return (type(o), type(o).__name__, pydevd_resolver.jyArrayResolver)    
        
        for t in typeMap.keys():            
            if isinstance(o, t):                
                return typeMap[t]
    except:
        traceback.print_exc()
        print typeMap
        print typeMap.__class__
        print dir( typeMap )
        
    #no match return default        
    return (type(o), type(o).__name__, pydevd_resolver.defaultResolver)


def makeValidXmlValue( s):
    return s.replace('<', '&lt;').replace('>', '&gt;')


def varToXML(v, name):
    """ single variable or dictionary to xml representation """
    type, typeName, resolver = getType(v)    
    
    try:
        value = str(v)
    except:
        try:
            value = `v`
        except:
            value = 'Unable to get repr for %s' % v.__class__
    
    xml = '<var name="%s" type="%s"' % (name, typeName)
    
    if value: 
        xmlValue = ' value="%s"' % (makeValidXmlValue(urllib.quote(value, '/>_= \t')))
    else:
        xmlValue = ''
        
    if resolver is not None: 
        xmlCont = ' isContainer="True"'
    else:
        xmlCont = ''
        
    return ''.join((xml, xmlValue, xmlCont, ' />\n'))

def frameVarsToXML(frame):
    """ dumps frame variables to XML
    <var name="var_name" scope="local" type="type" value="value"/>
    """    
#    print "frameVarsToXML"
    xml = ""
    keys = frame.f_locals.keys()
    keys.sort()    
    for k in keys:
        try:            
            v = frame.f_locals[k]            
            xml += varToXML(v, str(k))
        except Exception, e:
            traceback.print_exc()
            print >>sys.stderr,"unexpected error, recovered safely", str(e)
    return xml


def findFrame(thread_id, frame_id):
    """ returns a frame on the thread that has a given frame_id """
    if thread_id != id(threading.currentThread()) : 
        raise VariableError("findFrame: must execute on same thread")
    
    curFrame = sys._getframe()
    if frame_id == "*": 
        return curFrame # any frame is specified with "*"
    
    lookingFor = int(frame_id)
    
    while curFrame != None and lookingFor != id(curFrame):
        curFrame = curFrame.f_back
        
    if curFrame == None : 
        raise VariableError("findFrame: frame not found")
    
    return curFrame

def resolveCompoundVariable(thread_id, frame_id, scope, attrs):
    """ returns the value of the compound variable as a dictionary"""    
    frame = findFrame(thread_id, frame_id)    
    attrList = attrs.split('\t')
    if scope == "GLOBAL":        
        var = frame.f_globals
        del attrList[0] # globals are special, and they get a single dummy unused attribute
    else:
        var = frame.f_locals

    for k in attrList:
        type, typeName, resolver = getType(var)              
        var = resolver.resolve(var, k)
    
    try:        
        type, typeName, resolver = getType(var)        
        return resolver.getDictionary(var)
    except:
        traceback.print_exc()
    
def evaluateExpression( thread_id, frame_id, expression ):
    """returns the result of the evaluated expression"""
    frame = findFrame(thread_id, frame_id)
    result = None    
    try:
        result = eval( expression, frame.f_globals, frame.f_locals )
    except Exception, e:
        result = str( e )
    return result
