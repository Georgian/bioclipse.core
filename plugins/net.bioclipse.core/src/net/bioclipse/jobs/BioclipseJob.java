package net.bioclipse.jobs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.bioclipse.core.ResourcePathTransformer;
import net.bioclipse.core.SilentNotification;
import net.bioclipse.core.business.BioclipseException;
import net.bioclipse.core.util.LogUtils;
import net.bioclipse.managers.business.IBioclipseManager;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.WorkbenchJob;


/**
 * @author jonalv
 *
 */
public class BioclipseJob<T> extends Job {

    private volatile Method methodToRun;
    private volatile Method methodCalled;
    private volatile MethodInvocation invocation;
    private volatile boolean newWay;
    private volatile Object returnValue = NULLVALUE;
    private volatile IBioclipseManager bioclipseManager;
    
    private volatile Object[] arguments;

    private static Logger logger = Logger.getLogger( BioclipseJob.class );
    
    private BioclipseJobUpdateHook<Object> hook = null;
    
    public BioclipseJob( String name, 
                         Method methodToBeInvocated, 
                         MethodInvocation originalInvocation ) {
        super( name );
        this.setMethod( methodToBeInvocated );
        this.invocation = originalInvocation;
        newWay = false;
    }
    
    public BioclipseJob(String name) {
        super( name );
        newWay = true;
    }

    public static final Object NULLVALUE = new Object();
    
    protected IStatus run( IProgressMonitor monitor ) {

        if ( newWay ) {
            return runNewWay(monitor);
        }
        
        Object[] args;
        try {
            if ( getMethod() != getInvocation().getMethod() ) {
                /*
                 * Setup args array
                 */
                boolean hasUIJob = false;
                for ( Object o : getInvocation().getArguments() ) {
                    if ( o instanceof BioclipseUIJob ) {
                        hasUIJob = true;
                        break;
                    }
                }
                if (hasUIJob) {
                    args = new Object[ getInvocation().getArguments().length ];
                }
                else { 
                    args = new Object[ getInvocation().getArguments()
                                                      .length + 1 ];
                }
                args[args.length-1] = monitor;
                System.arraycopy( getInvocation().getArguments(), 
                                  0, 
                                  args, 
                                  0, 
                                  args.length - 1 );
                /*
                 * Then substitute from String to IFile where suitable
                 */
                for ( int i = 0; i < args.length; i++ ) {
                    Object arg = args[i];
                    if ( arg instanceof String &&
                        getMethod().getParameterTypes()[i] == IFile.class ) {
                         
                        args[i] = ResourcePathTransformer
                                  .getInstance()
                                  .transform( (String) arg );
                    }
                }
            }
            else {
                args = getInvocation().getArguments();
                monitor.beginTask( "", IProgressMonitor.UNKNOWN );
            }
        
            returnValue = getMethod().invoke( getInvocation().getThis(), args );
            
            int i = Arrays.asList( getInvocation().getMethod()
                                                  .getParameterTypes() )
                          .indexOf( BioclipseUIJob.class );

            final BioclipseUIJob uiJob ;
            
            if ( i != -1 )
                uiJob = ( (BioclipseUIJob)getInvocation().getArguments()[i] );
            else {
                uiJob = null;
            }
            
            if ( uiJob != null ) {
                uiJob.setReturnValue( returnValue );
                new WorkbenchJob("Refresh") {
        
                    @Override
                    public IStatus runInUIThread( 
                            IProgressMonitor monitor ) {
                        uiJob.runInUI();
                        return Status.OK_STATUS;
                    }
                    
                }.schedule();
            }
        } 
        catch ( Exception e ) {
            returnValue = e;
            if (e instanceof InvocationTargetException) {
                returnValue = e.getCause();
                if ( e.getCause() instanceof OperationCanceledException ) {
                    throw (OperationCanceledException)e.getCause();
                }
            }
            if (e instanceof OperationCanceledException) {
                throw (OperationCanceledException)e;
            }
            LogUtils.handleException( 
                new RuntimeException( 
                    "Exception occured: " + e.getClass().getSimpleName() + " - " 
                    + e.getMessage() + " while attempting to run " 
                    + bioclipseManager != null 
                      ? "<unknown>" 
                      : bioclipseManager.getManagerName() 
                    + "." + getMethod().getName() + " taking " 
                    + Arrays.deepToString( getMethod().getParameterTypes() ),
                    e ), logger, "net.bioclipse.managers" );
        }
        finally {
            monitor.done();
        }
        
        return Status.OK_STATUS;
    }

    @SuppressWarnings("unchecked")
    private IStatus runNewWay( IProgressMonitor monitor ) {
        
        long startTime = System.currentTimeMillis();
        try {
            final List<Object> newArguments = new ArrayList<Object>();
            newArguments.addAll( Arrays.asList( this.arguments ) );
            
            boolean usingReturner = false;
            final ReturnCollector returnCollector = new ReturnCollector();
            //add partial returner
            for ( Class<?> param : methodToRun.getParameterTypes() ) {
                if ( param == IReturner.class ) {
                    usingReturner = true;
                    int returnerPos = -1;
                    for ( Object o : newArguments ) {
                        if ( o instanceof IReturner ) {
                            returnerPos = newArguments.indexOf( o );
                        }
                    }
                    if ( returnerPos == -1 )  {
                        newArguments.add( returnCollector );
                    }
                    //If doing a complete return both the hook and the returner
                    //needs to be called. So decorating the original with a new 
                    //ReturnCollector that calls both for the complete return.
                    else {
                        final int finalReturnerPos = returnerPos;
                        newArguments.set( returnerPos, new ReturnCollector() {
                            IReturner collector;
                            {
                                collector 
                                    = ( (IReturner)
                                      newArguments.get( finalReturnerPos ) );
                            }
                            @Override
                            public void completeReturn( Object returnValue ) {
                                collector.completeReturn( returnValue );
                                returnCollector.completeReturn( returnValue );
                                super.completeReturn( returnValue );
                            }
                            @Override
                            public void partialReturn( Object o ) {
                                collector.partialReturn( o );
                                super.partialReturn( returnValue );
                            }
                        });
                    }
                }
            }
            
            //remove partial returner if needed
            if ( !usingReturner ) {
                int toBeremoved = -1;
                for ( int i = 0; i < arguments.length; i++ ) {
                    if ( newArguments.get(i) instanceof IReturner ) {
                        toBeremoved = i;
                    }
                }
                if ( toBeremoved != -1 ) {
                    Object o = newArguments.remove( toBeremoved );
                    if ( o instanceof BioclipseJobUpdateHook ) {
                        hook = (BioclipseJobUpdateHook<Object>) o;
                    }
                }
            }
            
            int i = Arrays.asList( methodCalled.getParameterTypes() )
                          .indexOf( BioclipseUIJob.class );
    
            final BioclipseUIJob uiJob ;
            
            if ( i != -1 )
                uiJob = ( (BioclipseUIJob)arguments[i] );
            else {
                uiJob = null;
            }
            if ( uiJob != null ) {
                newArguments.remove( uiJob );
            }

            newArguments.add( monitor );
            
            arguments = newArguments.toArray();
            //translate String -> IFile
            for ( int j = 0; j < arguments.length; j++ ) {
                if ( arguments[j] instanceof String &&
                     methodToRun.getParameterTypes()[j] == IFile.class ) {
                    arguments[j] = ResourcePathTransformer.getInstance()
                                       .transform( (String)arguments[j] );
                }
            }

            returnValue = methodToRun.invoke( bioclipseManager, 
                                              arguments );
            
            if (hook != null) {
                hook.completeReturn( returnValue );
            }
            
            if ( usingReturner ) {
                returnValue = returnCollector.getReturnValue();
                if ( returnValue == null ) {
                    returnValue = returnCollector.getReturnValues();
                }
            }
            
            if ( uiJob != null ) {
                uiJob.setReturnValue( returnValue );
                SilentNotification a 
                    = this.methodCalled
                          .getAnnotation( SilentNotification.class );
                if ( a != null &&
                     System.currentTimeMillis() - startTime 
                         > a.silentAfter() ) {
                    setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
                   monitor.subTask( a.message() );
                    
                   setProperty( IProgressConstants.ACTION_PROPERTY, 
                                 new Action( a.message() ) {
                                    public void run() {
                                        uiJob.runInUI();
                                    }
                                 }
                    );
                }
                else {
                    Display.getDefault().asyncExec( new Runnable() {
                        public void run() {
                            uiJob.runInUI();
                        }
                    });
                }
            }
        } 
        catch ( Exception e ) {
            
            returnValue = e;
            if (e instanceof InvocationTargetException) {
                returnValue = e.getCause();
            }

            Throwable t = e;
            while ( t != null ) {
                if ( t instanceof OperationCanceledException ) {
                    throw (OperationCanceledException)t;
                }
                t = t.getCause();
            }
            
            if ( LogUtils.findRootOrBioclipseException( e ) 
                 instanceof BioclipseException ) {
                LogUtils.handleException( e, logger, "net.bioclipse.managers" );
            }
            else {
                LogUtils.handleException( new RuntimeException( 
                    "Exception occured: " + e.getClass().getSimpleName() 
                      + " - " + e.getMessage() + " when attempting to run "
                      + bioclipseManager.getManagerName() + "." 
                      + getMethod().getName() + " taking: " 
                      + Arrays.deepToString( getMethod().getParameterTypes() )
                      + ". It was called with: " 
                      + Arrays.deepToString( arguments ),
                    e ), logger, "net.bioclipse.managers" );
            }
        }
        finally {
            monitor.done();
        }
        
        return Status.OK_STATUS;
    }

    @SuppressWarnings("unchecked")
    public synchronized T getReturnValue() {
        if ( returnValue == NULLVALUE ) {
            throw new IllegalStateException( "There is no return value" );
        }
        return (T)returnValue;
    }

    public void setMethod( Method method ) {
        this.methodToRun = method;
    }

    public Method getMethod() {
        return methodToRun;
    }

    public void setArguments(Object[] arguemtns) {
        this.arguments = arguemtns;
    }

    public MethodInvocation getInvocation() {
        return invocation;
    }

    public void setBioclipseManager( IBioclipseManager manager ) {
        this.bioclipseManager = manager;
    }
    
    public void setMethodCalled( Method methodCalled ) {

        this.methodCalled = methodCalled;
    }

    public Method getMethodCalled() {

        return methodCalled;
    }

    private static class ReturnCollector<T> implements IReturner<T> {

        private volatile T returnValue;
        private List<T> returnValues = new ArrayList<T>();
        
        public void partialReturn( T object ) {
            synchronized ( returnValues ) {
                returnValues.add( object );
            }
        }
        
        public List<T> getReturnValues() {
            synchronized ( returnValues ) {
                return returnValues;
            }
        }

        public void completeReturn( T object ) {
            returnValue=object;
        }
        
        public T getReturnValue() {
            return returnValue;
        }    
    }
}
