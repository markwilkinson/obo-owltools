package owltools.gaf;

import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Shahid Manzoor
 *
 */
public class GeneAnnotation {

	protected String bioentity;
	protected Bioentity bioentityObject;
	protected boolean isContributesTo;
	protected boolean isIntegralTo;
	protected String compositeQualifier;
	protected String relation;
	protected String cls;
	protected String referenceId;	
	protected String evidenceCls;
	protected String withExpression;
	protected String actsOnTaxonId;
	protected String lastUpdateDate; //TODO: convert it to date
	protected String assignedBy;
	protected String extensionExpression;
	protected String geneProductForm;
	protected String gafDocument;
	
	protected List<WithInfo> withInfoList;
	protected List<ExtensionExpression> extensionExpressionList;
	protected List<CompositeQualifier> compositeQualifierList;

	
	protected transient GafDocument gafDocumentObject;
	protected transient AnnotationSource annotationSource;
	
	/**
	 * If value of this variable is true then toString is re-calculated
	 */
	protected boolean isChanged;
	
	protected String toString;
	
	/**
	 * this method generates/updates the tab separated row of a gene annotation.
	 */
	protected void buildRow(){
		if(!isChanged)
			return;
		
		StringBuilder s = new StringBuilder();

		String taxon = "";
		String dbObjectSynonym = "";
		String dbObjectName = "";
		String dbObjectType = "";
		String symbol = "";
		
		if(this.bioentityObject!= null){
			taxon = bioentityObject.getNcbiTaxonId();
			if(taxon != null){
				int i = taxon.indexOf(":");
				
				if(i<0)
					i = 0;
				else
					i++;
				
				taxon ="taxon:" + bioentityObject.getNcbiTaxonId().substring(i);
			}

			dbObjectName = this.bioentityObject.getFullName();
			dbObjectType = this.bioentityObject.getTypeCls();
			symbol = this.bioentityObject.getSymbol();
		}
		
		if(this.bioentity != null){
			int i = bioentity.indexOf(":");
			if(i>-1){
				s.append(bioentity.substring(0, i)).append("\t").append(bioentity.substring(i+1)).append("\t");
			}else{
				s.append(bioentity).append("\t");
			}
		}else{
			s.append("\t\t");
		}
			
		
		s.append(symbol).append("\t");
		
		s.append(compositeQualifier).append("\t");
		
		s.append(this.cls).append("\t");
		
		s.append(this.referenceId).append("\t");
		
		s.append(this.evidenceCls).append("\t");
		
		s.append(this.withExpression).append("\t");
		
		s.append("\t");
		
		s.append(dbObjectName).append("\t");
		
		s.append(dbObjectSynonym).append("\t");
		
		s.append(dbObjectType).append("\t");
		
		if(this.actsOnTaxonId != null && this.actsOnTaxonId.length()>0){
			int i = actsOnTaxonId.indexOf(":");
			if(i<0)
				i = 0;
			else 
				i++;
			
			taxon += "|taxon:" + actsOnTaxonId.substring(i);
		}
		
		s.append(taxon).append("\t");
		
		s.append(this.lastUpdateDate).append("\t");
		
		s.append(this.assignedBy).append("\t");
		
		s.append(this.extensionExpression).append("\t");
		
		s.append(this.geneProductForm);
		
		this.isChanged = false;
		
		this.toString = s.toString();

	}
	
	public String toString(){
		buildRow();
		
		return toString;
	}
	
	
	public GeneAnnotation(){
		this("", false, false, "", "", "", "", "", "", "", "", "", "", "");
	}
	
	
	void setGafDocumetObject(GafDocument gafDocumentObject){
		this.gafDocumentObject = gafDocumentObject;
	}
	
	
	public GeneAnnotation(String bioentity, boolean isContributesTo,
			boolean isIntegralTo, String compositeQualifier, String cls,
			String referenceId, String evidenceCls, String withExpression,
			String actsOnTaxonId, String lastUpdateDate, String assignedBy,
			String extensionExpression, String geneProductForm,
			String gafDocument) {

		this.bioentity = bioentity;
		this.isContributesTo = isContributesTo;
		this.isIntegralTo = isIntegralTo;
		this.compositeQualifier = compositeQualifier;
		this.cls = cls;
		this.referenceId = referenceId;
		this.evidenceCls = evidenceCls;
		this.withExpression = withExpression;
		this.actsOnTaxonId = actsOnTaxonId;
		this.lastUpdateDate = lastUpdateDate;
		this.assignedBy = assignedBy;
		this.extensionExpression = extensionExpression;
		this.geneProductForm = geneProductForm;
		this.gafDocument = gafDocument;
		this.isChanged = true;
	}



	public String getBioentity() {
		return bioentity;
	}

	public void setBioentity(String bioentity) {
		this.bioentity = bioentity;
		
		this.isChanged = true;
	}

	
	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}

	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
		this.isChanged = true;
	
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
		this.isChanged = true;

	}

	public String getEvidenceCls() {
		return evidenceCls;
	}

	public void setEvidenceCls(String evidenceCls) {
		this.evidenceCls = evidenceCls;
		this.isChanged = true;

	}
	
	public String getWithExpression() {
		return withExpression;
	}

	public void setWithExpression(String withExpression) {
		this.withExpression = withExpression;
		this.isChanged = true;

	}

	public String getActsOnTaxonId() {
		return actsOnTaxonId;
	}

	public void setActsOnTaxonId(String actsOnTaxonId) {
		this.actsOnTaxonId = actsOnTaxonId;
		this.isChanged = true;

	}

	public String getLastUpdateDate() {
		return lastUpdateDate;
	}

	public void setLastUpdateDate(String lastUpdateDate) {
		this.lastUpdateDate = lastUpdateDate;
		this.isChanged = true;

	}

	public String getAssignedBy() {
		return assignedBy;
	}

	public void setAssignedBy(String assignedBy) {
		this.assignedBy = assignedBy;
		this.isChanged = true;

	}

	public String getExtensionExpression() {
		return extensionExpression;
	}

	public void setExtensionExpression(String extensionExpression) {
		this.extensionExpression = extensionExpression;
		this.isChanged = true;

	}

	public String getGeneProductForm() {
		return geneProductForm;
	}

	public void setGeneProductForm(String geneProductForm) {
		this.geneProductForm = geneProductForm;
		this.isChanged = true;

	}


	public String getCompositeQualifier() {
		return compositeQualifier;
	}

	public void setCompositeQualifier(String compositeQualifier) {
		this.compositeQualifier = compositeQualifier;
		this.isChanged = true;

	}

	public Bioentity getBioentityObject() {
		
		return bioentityObject;
	}

	
	public void setBioentityObject(Bioentity bioentityObject) {
		this.bioentityObject = bioentityObject;
		this.isChanged = true;
	}
	
	
	public String getGafDocument() {
		return gafDocument;
	}

	public void setGafDocument(String gafDocument) {
		this.gafDocument = gafDocument;
	}

	public boolean getIsContributesTo() {
		return isContributesTo;
	}

	public void setIsContributesTo(boolean isContributesTo) {
		this.isContributesTo = isContributesTo;
	}

	public boolean getIsIntegralTo() {
		return isIntegralTo;
	}

	public void setIsIntegralTo(boolean isIntegralTo) {
		this.isIntegralTo = isIntegralTo;
	}

	public List<ExtensionExpression> getExtensionExpressions(){
		if(extensionExpressionList == null){
			
			if(gafDocumentObject != null){
				extensionExpressionList = gafDocumentObject.getExpressions(getExtensionExpression());
				
				if(extensionExpressionList == null)
					extensionExpressionList = Collections.emptyList();
			}
		}
		
		return extensionExpressionList;
	}
	
	public List<WithInfo> getWithInfos(){
		if(withInfoList == null){
			
			if(gafDocumentObject != null)
				withInfoList = gafDocumentObject.getWithInfos(getWithExpression());
			
			
			if(withInfoList == null){
				withInfoList = Collections.emptyList();
			}
		}
		
		return withInfoList;
	}
	
	public List<CompositeQualifier> getCompositeQualifiers(){
		if(compositeQualifierList == null){
			if(gafDocumentObject != null){
				compositeQualifierList = gafDocumentObject.getCompositeQualifiers(getCompositeQualifier());
			}

			if(compositeQualifierList == null)
				compositeQualifierList = Collections.emptyList();
		}
		return compositeQualifierList;
	}

	public AnnotationSource getSource() {
		return annotationSource;
	}

	void setSource(AnnotationSource annotationSource) {
		this.annotationSource = annotationSource;
		this.toString = annotationSource.getRow();
		isChanged = false;
	}

}
