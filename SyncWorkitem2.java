1package com.pasa.rtc;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.ibm.team.process.client.IProcessItemService;
import com.ibm.team.process.common.IProjectArea;
import com.ibm.team.process.common.IProjectAreaHandle;
import com.ibm.team.repository.client.ITeamRepository;
import com.ibm.team.repository.client.TeamPlatform;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.query.IQueryResult;
import com.ibm.team.workitem.common.query.IResult;
import com.ibm.team.workitem.client.IAuditableClient;
import com.ibm.team.workitem.client.IQueryClient;
import com.ibm.team.workitem.client.IWorkItemClient;
import com.ibm.team.workitem.common.IAuditableCommon;
import com.ibm.team.workitem.common.IQueryCommon;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.expression.AttributeExpression;
import com.ibm.team.workitem.common.expression.IQueryableAttribute;
import com.ibm.team.workitem.common.expression.IQueryableAttributeFactory;
import com.ibm.team.workitem.common.expression.Term;
import com.ibm.team.workitem.common.expression.Term.Operator;
import com.ibm.team.workitem.common.model.AttributeOperation;
import com.ibm.team.workitem.common.model.ItemProfile;
import com.ibm.team.workitem.common.model.IAttribute;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.query.IResolvedResult;
import com.pasa.rtc.SynchronizeAttributes.UnResolvedPageJob;

/**
 * Automation script to persist workitems from CCM1 to CCM4 based on workitem summary to enable synchronization b/w CCM4 & CCM5. 
 * 
 * @author kumarvik
 *
 */
public class SyncWorkItem {
	
	private static String CCM1_REPOSITORY_ADDRESS;
	private static String CCM4_REPOSITORY_ADDRESS;
	private static String USER;
	private static String PASSWORD;
	private static String typeId;
	private static List<IWorkItem> ccm1_wis;
	private static List<IWorkItem> ccm4_wis;
    private static IWorkItemClient wiclient1;
    private static IWorkItemClient wiclient4;
    private static ITeamRepository repo1;
    private static ITeamRepository repo4;
    
    
    public static void main(String[] args) {
    	boolean retVal = false;
		if (args.length == 5){
			CCM1_REPOSITORY_ADDRESS = args[0];
			USER = args[1];
			PASSWORD = args[2];
				
			typeId = args[4];
			retVal = new SyncWorkItem().runme(CCM1_REPOSITORY_ADDRESS, USER, PASSWORD, args[3], typeId);
		}else{
			retVal = false;
			//TODO: write the meaningful sysout.
			//System.err.println("Invald Parameters.  \n\nUsage: java -jar TimeTracking.jar <REPOSITORY_ADDRESS> <USER> <Passfile> <proj_name> <typeId>");
		}
		System.out.println("Finished Without Errors?: " + retVal);
		if (retVal){
			System.exit(0);
		}else{
			System.exit(1);
		}
       
    }
    
    public boolean runme(String REPOSITORY_ADDRESS, String USER, String Pass, String proj_name, String typeId){
   	 TeamPlatform.startup();
        try {     
            IProgressMonitor monitor = new NullProgressMonitor();
            //login to ccm1
            repo1 = login(monitor, CCM1_REPOSITORY_ADDRESS);
           //login to ccm4
            repo4 = login(monitor, CCM4_REPOSITORY_ADDRESS);
            wiclient1 = (IWorkItemClient) repo1.getClientLibrary(IWorkItemClient.class);
            wiclient4 = (IWorkItemClient) repo4.getClientLibrary(IWorkItemClient.class);
            
            IProjectArea proj = getProjectArea(proj_name);
            System.out.println(proj_name);
            IAuditableClient auditable1= (IAuditableClient) repo1.getClientLibrary(IAuditableClient.class);
            IAuditableClient auditable4= (IAuditableClient) repo4.getClientLibrary(IAuditableClient.class);
            System.out.println(typeId);
            
             // fetch workitems from CCM1
             ccm1_wis = findAllWorkItems(typeId,proj,monitor, auditable1);
             // fetch workitems from CCM4
             ccm4_wis = findAllWorkItems(typeId,proj,monitor, auditable4);
            
            System.out.println("Found: "+ccm1_wis.size()+" Work items");
            System.out.println("Found: "+ccm4_wis.size()+" Work items");
            
            Iterator wiiter1 = ccm1_wis.iterator();
            while(wiiter1.hasNext()){
            	IWorkItem wi = (IWorkItem) wiiter1.next();
            	IWorkItemCommon workItemCommon = (IWorkItemCommon) repo1.getClientLibrary(IWorkItemCommon.class);
            	
            	//TODO:Define the set of attributes to get values from workitems  
            	IAttribute summaryAttribute= workItemCommon.findAttribute(proj, "com.panasonic.team.workitem.attribute.**********", monitor);
            	IAttribute linkAttribute= workItemCommon.findAttribute(proj, "com.panasonic.team.workitem.attribute.************", monitor);
            	
            	Integer value = null;
           		if(wi.isAttributeSet(summaryAttribute) && wi.isAttributeSet(linkAttribute)){
           			//TODO: get each attribute value
               		value= (Integer)wi.getValue(attribute);
               	}else{
               		value = new Integer(0);
               	}
           		//TODO: Iterator workitems from CCM4 and match the summary attribute value and run query to update the workitem. skip if no Summary found
            }
            
        } catch (TeamRepositoryException e) {
            System.out.println("Unable to login: " + e.getMessage());
        }catch (Exception e){
       	 System.err.println("UNKNOWN EXCEPTION: "+e.getMessage());
       	 e.printStackTrace();
        } finally{
            TeamPlatform.shutdown();
        }
        return true;
   }
    
    
	public static void getLinksByWorkItems(){
		final ILinkManager linkManager = (ILinkManager) repo.getClientLibrary(ILinkManager.class);
		final IReference workItemRef = linkManager.referenceFactory().createReferenceToItem(workItemHandle);
		final ILinkQueryPage queryPage = linkManager.findLinksBySource(linkType, workItemRef, monitor);
		final Collection<ILink> links = queryPage.getAllLinksFromHereOn();
		//TODO : add these links to worklitem list
	    /*for(ILink l : links){  
	      println("Link is: " + l.getLinkTypeId() + ", url: " + l.getThisEndpointDescriptor(l.getTargetRef()).getIcon());
	   }*/
    }
	
	public static void getAttachmentsByWorkItems(){
		IWorkItem workItem = workItemClient.findWorkItemById(id, IWorkItem.FULL_PROFILE, null);

		IWorkItemCommon common = (IWorkItemCommon) teamRepository.getClientLibrary(IWorkItemCommon.class);
		IWorkItemReferences workitemReferences = common.resolveWorkItemReferences(workItem, null);
		List references = workitemReferences.getReferences(WorkItemEndPoints.ATTACHMENT);
		//TODO : add attachemnt referenes to workitem list
		for (IReference iReference : references) {
			IAttachmentHandle attachHandle = (IAttachmentHandle) iReference.resolve();
			IAuditableClient auditableClient = (IAuditableClient) teamRepository.getClientLibrary(IAuditableClient.class);
			IAttachment attachment = (IAttachment) auditableClient.resolveAuditable((IAttachmentHandle) attachHandle,
				IAttachment.DEFAULT_PROFILE, null);
		}
	}
    
    
    /**
     * @param monitor
     * @return
     * @throws TeamRepositoryException
     */
    public static ITeamRepository login(IProgressMonitor monitor, String REPOSITORY_ADDRESS) throws TeamRepositoryException {
        ITeamRepository repository = TeamPlatform.getTeamRepositoryService().getTeamRepository(REPOSITORY_ADDRESS);
        repository.registerLoginHandler(new ITeamRepository.ILoginHandler() {
            public ILoginInfo challenge(ITeamRepository repository) {
                return new ILoginInfo() {
                    public String getUserId() {
                        return USER;
                    }
                    public String getPassword() {
                        return PASSWORD;                        
                    }
                };
            }
        });
        System.out.println("Contacting " + repository.getRepositoryURI() + "...");
        repository.login(monitor);
        System.out.println("Connected");
        return repository;
    }
    
    /**
     * @param projectName
     * @return
     */
    public static IProjectArea getProjectArea(String projectName){
    	IProgressMonitor monitor = new NullProgressMonitor();
    	IProcessItemService service= (IProcessItemService) repo1.getClientLibrary(IProcessItemService.class);
    	Iterator<IProjectArea> foundProjs = null;
    	try {
    		foundProjs = service.findAllProjectAreas(null, monitor).iterator();
    	} catch (TeamRepositoryException e) {
    		e.printStackTrace();
    	}
    	
    	IProjectArea result = null;
    	while (foundProjs.hasNext() && result == null){
    		IProjectArea proj = foundProjs.next();
    		//System.out.println("Project:"+proj.getName());
    		if (projectName.equals(proj.getName()))  {
    			result = proj;
    		}
    	
    	}
    	return result;
    }
    
    
    
    private static List<IWorkItem> findAllWorkItems(String type, IProjectAreaHandle projectArea, IProgressMonitor monitor, IAuditableCommon auditable) throws TeamRepositoryException {
    	ccm1_wis = new ArrayList<IWorkItem>();
    	//TODO: Prepare queryable attributes
    	/*IQueryableAttribute typeAtt = findQueryableAttribute(projectArea, IWorkItem.FULL_PROFILE, monitor, auditable);
    	IQueryableAttribute timeSpentAtt = findQueryableAttribute(projectArea, "com.ibm.team.workitem.attribute.timespent",monitor,auditable);
    	IQueryableAttribute creationDate= findQueryableAttribute(projectArea, IWorkItem.CREATION_DATE_PROPERTY, monitor, auditable);
    	IQueryableAttribute projectAreaAttribute= findQueryableAttribute(projectArea, IWorkItem.PROJECT_AREA_PROPERTY, monitor, auditable);*/
    	
    	//TODO: Prepare Attribute expression
    	/*AttributeExpression projectAreaExpression= new AttributeExpression(projectAreaAttribute, AttributeOperation.EQUALS, projectArea);
    	AttributeExpression typeExpression =new AttributeExpression(typeAtt,AttributeOperation.EQUALS,type);*/
    	
    	//TODO: Define the search query condition
    	Term term= new Term(Operator.AND);
    	term.add(projectAreaExpression);
    	term.add(typeExpression);
    	
    	IWorkItemClient workItemClient = (IWorkItemClient) repo1.getClientLibrary(IWorkItemClient.class);
    	IQueryClient queryClient = workItemClient.getQueryClient();
    	IQueryResult unresolvedResults = queryClient.getQueryResults(query);
    	
    	ItemProfile<IWorkItem> profile = IWorkItem.FULL_PROFILE;
    	IQueryCommon queryService= (IQueryCommon) repo1.getClientLibrary(IQueryClient.class);

    	//TODO : Change queryService method call to UnReslovedExpressionResults() and pass the arguments
    	IQueryResult<IResult> result = queryService.getExpressionResults(projectArea, term);		
    	result.setPageSize(100);
    	result.setLimit(100000);
    	processUnresolvedResults(projectArea,unresolvedResults,profile,monitor,ccm1_wis);
    	
  	return ccm1_wis;
    }
    
    public static void processUnresolvedResults(IProjectArea projectArea , IQueryResult<IResults> results,
    		ItemProfile profile, IProgressMonitor monitor, List<IWorkItem> workItemsList)
    		throws TeamRepositoryException {
    	// Get the required client libraries
    	ITeamRepository teamRepository = (ITeamRepository)projectArea.getOrigin();
    	IWorkItemClient workItemClient = (IWorkItemClient) teamRepository.getClientLibrary(IWorkItemClient.class);
    	IAuditableCommon auditableCommon = (IAuditableCommon) teamRepository.getClientLibrary(IAuditableCommon.class);
    	long processed = 0;
    	while (results.hasNext(monitor)) {
    		IResult result = (IResult) results.next(monitor);
    		IWorkItem workItem = auditableCommon.resolveAuditable((IAuditableHandle) result.getItem(), profile, monitor);
    		// Do something with the work item here
    		workItemsList.add(workItem);
    		processed++;
    	}
    	System.out.println("Processedlts: " + processed);
    }
    
    
    
    private static IQueryableAttribute findQueryableAttribute(IProjectAreaHandle projectArea, String attributeId, IProgressMonitor monitor, IAuditableCommon auditable) throws TeamRepositoryException {
		IQueryableAttributeFactory factory= QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
		return factory.findAttribute(projectArea, attributeId, auditable, monitor);
	}
    public static class ResolvedPageJob extends Job {
    	private List<IResolvedResult<IWorkItem>> fPage;
    	private ITeamRepository fTeamRepository;
    	private IWorkItemClient fWorkItemClient;

    	public ResolvedPageJob( ITeamRepository teamRepository, IWorkItemClient workitemClient, List<IResolvedResult<IWorkItem>> results) {
    		super("Run Page");
    		fTeamRepository=teamRepository;
    		fWorkItemClient=workitemClient;
    		fPage=results;
    	}

    	protected IStatus run(IProgressMonitor monitor) {
    		try {
    			processResolvedResultsPaged(fTeamRepository, fWorkItemClient,fPage);
    		} catch (TeamRepositoryException e) {
    			//System.out.println("Exception "+e.getLocalizedMessage());
    			return new Status(Status.ERROR,"Me","TeamRepositoryException");
    		}
    		return null;
    	}
    }
    public static void processResolvedResultsPaged(ITeamRepository teamRepository,
    		IWorkItemClient workItemClient,
    		List page)
    		throws TeamRepositoryException {
    	long processed = 0;
    	Iterator pages = page.iterator();
    	while (pages.hasNext()){
    		IResolvedResult resolvedResult = (IResolvedResult) pages.next();
    		IWorkItem workItem = (IWorkItem) resolvedResult.getItem();
    		ccm1_wis.add(workItem);
    		// do something with the work item
    		processed++;
    	}
//    	System.out.println("Processedlts: " + processed);
    }
}

