
helperPack = Packages.org.cougaar.core.plugin.util;
planPack = Packages.org.cougaar.planning.ldm.plan;
jglPack = Packages.com.objectspace.jgl;
genericPack= Packages.org.cougaar.mlm.plugin.generic;

waitingForSub = null;
subOrgAssets = null;
allocatableGLSTask = null;
myExpansions = null;
myAllocs = null;

function scriptSubscription() {

    subOrgPred = genericPack.GenericScriptHelper.getSubOrgPredicate();
    allocGLSPred = genericPack.GenericScriptHelper.getAllocatableGLSPredicate();
    expChangePred = genericPack.GenericScriptHelper.getExpChangePred();
    allocChangePred = helperPack.PredicateFactory.newAllocationsPredicate( "GETLOGSUPPORT", ldmf );

    writeln( "the javatypeof suborgpred is :" );
    writeln( javaTypeOf( subOrgPred ) );

    arr = new Array();
    arr[0] = allocGLSPred;
    arr[1] = subOrgPred;
    arr[2] = expChangePred;
    arr[3] = allocChangePred;
    return arr;
}

function scriptExecute( mySubscriptions ) {
    allocatableGLSTask = mySubscriptions[0];
    subOrgAssets = mySubscriptions[1];
    myExpansions = mySubscriptions[2];
    myAllocations = mySubscriptions[3];

    if (allocatableGLSTask.hasChanged() ) {
	newTasks = allocatableGLSTask.getAddedList();
	while (newTasks.hasMoreElements()) {
	    allocate( newTasks.nextElement());
	}
    }

    if ( myExpansions.hasChanged() ) 
	helperPack.ExpanderHelper.updateAllocationResult( myExpansions );

    if ( myAllocations.hasChanged() )
	helperPack.AllocatorHelper.updateAllocationResult( myAllocations );

}

function allocate( t ) {

    subtasks = new Packages.java.util.Vector();
    wf = ldmf.newWorkflow();
    wf.setIsPropagatingToSubtasks();
    esubs = subOrgAssets.elements();
    while ( esubs.hasMoreElements() ) {
	orga = esubs.nextElement();
	newtask = createSubTask( t, orga );
	subtasks.addElement( newtask );
    }
    newexp = helperPack.ExpanderHelper.wireExpansion( subtasks, ldmf, t, wf ); 
    helperPack.ExpanderHelper.publishAddExpansion( newexp ); 
    helperPack.AllocatorHelper.allocateToIndirectObjects( wf, ldmf, getSubscriber() );
}


function createSubTask( t,  subasset) {
    prepphrases = new Packages.java.util.Vector();
    subtask = ldmf.newTask();
 
    // pull out the "with OPlan" prep phrase and store for later
    origpp = t.getPrepositionalPhrases();
    while (origpp.hasMoreElements()) {
	theorigpp = origpp.nextElement();
	if ( theorigpp.getPreposition() ==  planPack.Preposition.WITH ) {  
	    prepphrases.addElement(theorigpp);
	}
    }
	  
    newpp = ldmf.newPrepositionalPhrase();
    newpp.setPreposition( planPack.Preposition.FOR ); 
    newpp.setIndirectObject(subasset);
    prepphrases.addElement(newpp);
    subtask.setPrepositionalPhrases(prepphrases.elements());
    subtask.setParentTask( t );
    subtask.setDirectObject( t.getDirectObject() );
    subtask.setVerb( t.getVerb() );
    subtask.setPlan( t.getPlan() );
    subtask.setPreferences( t.getPreferences() );
    return subtask;
}
