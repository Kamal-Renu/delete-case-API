import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeleteService {

    @Inject
	CaseRepository caseRepository;

    @Inject
	MongoRepository mongoRepository;

    @Inject
	ComplaintRepository complaintRepository;

    @Inject
	CommunicationServiceClient communicationServiceClient;

    public boolean deleteByCaseID(String caseID) {
        // Get case data using case ID
        Optional<Case> caseOptional = caseRepository.getCaseByCaseID(caseID);
        if(caseOptional.isEmpty()) {
            throw new CustomException("No case exist with this case ID");
        }

        // Check case is valid or not
        Map<String, List<String>> errorMap = new HashMap<>();
        boolean isValid = deleteCaseByCaseIDValidation(caseOptional.get(), errorMap);

        if(!isValid) {
            throw new CustomException(errorMap);
        }

        // Delete case using caseID
        boolean isCaseDeleted = deleteCaseHandler(caseOptional.get());

        if(isCaseDeleted) {
            communicationServiceClient.SendCorrespondence("Successfully deleted case", caseOptional.get());
        }

        return isCaseDeleted;
    }

    @Transactional
    private boolean deleteCaseHandler(Case case) {
        // Delete related case in case Table
        boolean isCaseDeleted = caseRepository.deleteByCaseNumber(case.getCaseNumber());

        // Delete related case in mongo collection
        boolean isMongoCaseDeleted = mongoRepository.deleteByCaseNumber(case.getCaseNumber());

        // Delete related complaint in complaint table
        boolean isComplaintDeleted = complaintRepository.deleteByCaseNumber(case.getCaseNumber());
        
        if(!isCaseDeleted && !isMongoCaseDeleted && !isComplaintDeleted) {
            // Log here
            return false;
        }
        return true;
    }

    private boolean deleteCaseByCaseIDValidation(Case case, Map<String, List<String>> errorMap) {
        // Get by enum class
        List<String> inValidStatusList = new ArrayList<String>();
        inValidStatusList.add("resolved");
        inValidStatusList.add("pending");

        List<String> errorList = new ArrayList<String>();

        // Status validation
        if(inValidStatusList.contains(case.getStatus())) {
            errorList.add("Invalid status");
            errorMap.put("status", errorList);
            return errorMap;
        }
        return errorMap;
    }

}