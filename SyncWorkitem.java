package com.fmr.PR000029.rtc.workitem.automation;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.fmr.PR000029.rtc.workitem.automation.TimeTracking.ResolvedPageJob;

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
    	IProcessItemService service= (IProcessItemService) repo.getClientLibrary(IProcessItemService.class);
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
    	IQueryableAttribute typeAtt = findQueryableAttribute(projectArea, IWorkItem.TYPE_PROPERTY, monitor, auditable);
    	IQueryableAttribute timeSpentAtt = findQueryableAttribute(projectArea, "com.ibm.team.workitem.attribute.timespent",monitor,auditable);
    	IQueryableAttribute creationDate= findQueryableAttribute(projectArea, IWorkItem.CREATION_DATE_PROPERTY, monitor, auditable);
    	IQueryableAttribute projectAreaAttribute= findQueryableAttribute(projectArea, IWorkItem.PROJECT_AREA_PROPERTY, monitor, auditable);
    	
    	//TODO: Prepare Attribute expression
    	AttributeExpression projectAreaExpression= new AttributeExpression(projectAreaAttribute, AttributeOperation.EQUALS, projectArea);
    	AttributeExpression typeExpression =new AttributeExpression(typeAtt,AttributeOperation.EQUALS,type);
    	
    	//TODO: Define the search query condition
    	Term term= new Term(Operator.AND);
    	term.add(projectAreaExpression);
    	term.add(typeExpression);
    	
    	ItemProfile<IWorkItem> profile = IWorkItem.FULL_PROFILE;
    	IQueryCommon queryService= (IQueryCommon) repo.getClientLibrary(IQueryClient.class);

    	//TODO : Change queryService method call to UnReslovedExpressionResults() and pass the arguments
    	IQueryResult<IResolvedResult<IWorkItem>> result = queryService.getResolvedExpressionResults(projectArea, term, profile);		
    	result.setPageSize(100);
    	result.setLimit(100000);
    	
    	ArrayList jobs = new ArrayList();
    	while(result.hasNext(null)){
    		//TODO: Change this to UnReslovedPageJob object.
    		ResolvedPageJob job = new ResolvedPageJob(repo, wiclient, result.nextPage(null));
    		jobs.add(job);
    		job.setUser(true);
    		job.schedule();
    	}
    	Iterator allJobs = jobs.iterator();
    	while (allJobs.hasNext()){
    		//TODO: Change this to UnReslovedPageJob object.
    		ResolvedPageJob resolvedPageJob = (ResolvedPageJob)allJobs.next();
    		try {
    			resolvedPageJob.join();
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
    	}
  	return wis;
    }
    
    private static IQueryableAttribute findQueryableAttribute(IProjectAreaHandle projectArea, String attributeId, IProgressMonitor monitor, IAuditableCommon auditable) throws TeamRepositoryException {
		IQueryableAttributeFactory factory= QueryableAttributes.getFactory(IWorkItem.ITEM_TYPE);
		return factory.findAttribute(projectArea, attributeId, auditable, monitor);
	}

}

