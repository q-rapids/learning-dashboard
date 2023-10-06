
var currentProject;
var currentProduct;
var previousSelectionId;
var currentSelectionId;
var projects;
var areProducts = false;
var serverUrl = sessionStorage.getItem("serverUrl");
var globalChecked;
var metrics = [];
var selectedStudent;
var tempId=-1;
var identities = [];


function loadIdentities(){
	let url = "/api/projects/identities";
	if (serverUrl) {
		url = serverUrl + url;
	}

	jQuery.ajax({
		dataType: "json",
		url: url,
		cache: false,
		type: "GET",
		async: true,
		success: function (data) {
			identities = data;
		}});
}

function buildFirstPartOfTree() {
	var url = "/api/projects";
	if (serverUrl) {
		url = serverUrl + url;
	}
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: true,
        success: function (data) {
        	var T = document.getElementById('productList');
        	productList.innerHTML = "";
        	var product = document.createElement('li');
        	product.classList.add("list-group-item");
        	//product.classList.add("Product");
        	var productP = document.createElement('p');
        	productP.setAttribute("data-toggle", "collapse");
        	productP.setAttribute("data-target", "#projectList");
        	productP.appendChild(document.createTextNode("All Projects"));
        	product.appendChild(productP);
        	productList.appendChild(product);
        	
        	var projectList = document.createElement('ul');
        	projectList.classList.add("collapse");
        	projectList.setAttribute("id", "projectList");
        	
        	for (var i = 0; i < data.length; i++) {
        		var project = document.createElement('li');
        		project.classList.add("list-group-item");
        		project.classList.add("Project");
        		project.appendChild(document.createTextNode(data[i].name));
        		project.setAttribute("id", ("project" + data[i].id));
        		project.addEventListener("click", clickOnTree);
                projectList.appendChild(project);
            }
        	productList.appendChild(projectList);
        	projects = data;
        	buildSecondPartOfTree();
        }
    });
};

function buildSecondPartOfTree() {
	var url = "/api/products";
	if (serverUrl) {
		url = serverUrl + url;
	}
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: true,
        success: function (data) {
        	var productList = document.getElementById('productList');        	
        	for (var i = 0; i < data.length; i++) {
        		var product = document.createElement('li');
            	product.classList.add("list-group-item");
            	product.classList.add("Product");
            	product.setAttribute("id", ("product" + data[i].id));
            	product.setAttribute("data-toggle", "collapse");
            	product.setAttribute("data-target", ("#sonsOf" + data[i].id));
            	product.appendChild(document.createTextNode(data[i].name));
            	product.addEventListener("click", clickOnTree);
            	            	
            	var projectList = document.createElement('ul');
            	projectList.classList.add("collapse");
            	projectList.setAttribute("id", ("sonsOf" + data[i].id));
            	for (var j = 0; j < data[i].projects.length; j++) {
            		var project = document.createElement('li');
            		project.classList.add("list-group-item");
            		project.classList.add("Project");
            		project.appendChild(document.createTextNode(data[i].projects[j].name));
            		project.setAttribute("id", ("project" + data[i].projects[j].id + "-childOf"+ data[i].name));
            		project.addEventListener("click", clickOnTree);
                    projectList.appendChild(project);
                }
            	productList.appendChild(product);      
            	productList.appendChild(projectList);
            }
        	document.getElementById('productTree').appendChild(productList);
        }
    });
};

function clickOnTree(e){
	if (e.target.classList.contains("Project")) {
		currentSelection = "Project";
		previousSelectionId = currentSelectionId;
		currentSelectionId = e.target.id;
		if (previousSelectionId != null) {
			document.getElementById(previousSelectionId).setAttribute('style', 'background-color: #ffffff;');
		}
		document.getElementById(currentSelectionId).setAttribute('style', 'background-color: #efeff8;');
		var idString = e.target.id.split("-")[0];
		getChosenProject(idString.replace("project", ""));
	} else if (e.target.classList.contains("Product")) {
		currentSelection = "Project";
		previousSelectionId = currentSelectionId;
		currentSelectionId = e.target.id;
		if (previousSelectionId != null) {
			document.getElementById(previousSelectionId).setAttribute('style', 'background-color: #ffffff;');
		}
		document.getElementById(currentSelectionId).setAttribute('style', 'background-color: #efeff8;');
		getChosenProduct(e.target.id.replace("product", ""));
	}
}




function getChosenProject(currentProjectId) {

	var url = "/api/projects/" + currentProjectId;
	if (serverUrl) {
		url = serverUrl + url;
	}
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: true,
        success: function (data) {
        	var projectForm = document.createElement('div');
    		projectForm.setAttribute("id", "projectForm");
			globalChecked=data.isGlobal
    		var title1Row = document.createElement('div');
    		title1Row.classList.add("productInfoRow");

    		var title1P = document.createElement('p');
    		title1P.appendChild(document.createTextNode("Project Information"));
    		title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    		title1Row.appendChild(title1P);
    		if(data.anonymized){
    		    // Create an img element and set its attributes
                let incognitoImg = document.createElement('img');
                incognitoImg.classList.add("icons");
                incognitoImg.setAttribute("src", "/icons/incognito.png");
                incognitoImg.setAttribute("style", "margin-right: 1%");
                title1Row.appendChild(incognitoImg);

    		}
    		projectForm.appendChild(title1Row);

    		if(data.anonymized){
                // Create the main div
                let incognitoDiv = document.createElement('div');

                // Create a text node
                let incognitoText = document.createTextNode('This project is anonymized');
                incognitoDiv.classList.add("productInfoRow");
                // Add text node to the div
                incognitoDiv.appendChild(incognitoText);
                projectForm.appendChild(incognitoDiv);
    		}

    		var idRow = document.createElement('div');
    		idRow.classList.add("productInfoRow");
    		var idP = document.createElement('p');
    		idP.appendChild(document.createTextNode("Assessment ID: "));
    		idP.setAttribute('style', 'font-size: 18px; margin-right: 1%; width: 40%;');
        	idRow.appendChild(idP);
    		var idP2 = document.createElement("p");
    		idP2.setAttribute('id', 'projectId');
    		idP2.setAttribute('style', 'font-size: 18px; margin-right: 1%; width: 100%;');
    		idP2.innerHTML = data.externalId;
    		idRow.appendChild(idP2);
    		projectForm.appendChild(idRow);
    		
    		var nameRow = document.createElement('div');
    		nameRow.classList.add("productInfoRow");
    		var nameP = document.createElement('p');
        	nameP.appendChild(document.createTextNode("Name*: "));
        	nameP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
        	nameRow.appendChild(nameP);
    		var inputName = document.createElement("input");
    		inputName.setAttribute('id', 'projectName');
    		inputName.setAttribute('type', 'text');
    		inputName.setAttribute('value', data.name);
    		inputName.setAttribute('style', 'width: 100%;');
    		inputName.setAttribute('placeholder', 'Write the project name here');
    		nameRow.appendChild(inputName);
    		projectForm.appendChild(nameRow);
    		
    		var descriptionRow = document.createElement('div');
    		descriptionRow.classList.add("productInfoRow");
    		descriptionRow.setAttribute('style', 'resize: vertical;');
    		var descriptionP = document.createElement('p');
    		descriptionP.appendChild(document.createTextNode("Description: "));
    		descriptionP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    		descriptionRow.appendChild(descriptionP);
    		var inputDescription = document.createElement("textarea");
    		inputDescription.setAttribute('id', 'projectDescription');
    		inputDescription.value= data.description;
    		inputDescription.setAttribute('style', 'width: 100%;');
    		inputDescription.setAttribute('rows', '3');
    		inputDescription.setAttribute('placeholder', 'Write the project description here');
    		descriptionRow.appendChild(inputDescription);
    		projectForm.appendChild(descriptionRow);

			var backlogIdRow = document.createElement('div');
			backlogIdRow.classList.add("productInfoRow");
			var backlogIdP = document.createElement('p');
			backlogIdP.appendChild(document.createTextNode("Backlog ID:"));
			backlogIdP.setAttribute('style', 'font-size: 18px; margin-right: 1%; width: 13%');
			backlogIdRow.appendChild(backlogIdP);
			var inputBacklogId = document.createElement("input");
			inputBacklogId.setAttribute('id', 'projectBacklogId');
			inputBacklogId.setAttribute('type', 'text');
			var backlogId = "";
			if (data.backlogId) backlogId = data.backlogId;
			inputBacklogId.setAttribute('value', backlogId);
			inputBacklogId.setAttribute('style', 'width: 100%;');
			inputBacklogId.setAttribute('placeholder', 'Write the project backlog ID here');
			backlogIdRow.appendChild(inputBacklogId);
			projectForm.appendChild(backlogIdRow);

			//LOAD PROJECT IDENTITIES

            identities.forEach(identity => {
                var identityRow = document.createElement('div');
                identityRow.classList.add("productInfoRow");
                var identityURLp = document.createElement('p');
                identityURLp.appendChild(document.createTextNode(identity +" URL:"));
                identityURLp.setAttribute('style', 'font-size: 18px; width: 15%');
                identityRow.appendChild(identityURLp);
                var inputIdentityUrl = document.createElement("input");

                inputIdentityUrl.setAttribute('id', 'input' + identity + 'Url');
                inputIdentityUrl.setAttribute('type', 'text');
                var identityURL = (data.identities[identity] !== undefined && data.identities[identity].url !== undefined)
                                        ? data.identities[identity].url : "";
                inputIdentityUrl.setAttribute('value', identityURL);
                inputIdentityUrl.setAttribute('style', 'width: 100%;');

                var placeholder_text = 'Write the ' + identity + ' URL';
                if(identity == "GITHUB") placeholder_text += '. In case there are more than one separate them by a ";"';
                inputIdentityUrl.setAttribute('placeholder',placeholder_text);
                identityRow.appendChild(inputIdentityUrl);
                projectForm.appendChild(identityRow);
            })


			var globalCheckRow = document.createElement("div");
			globalCheckRow.classList.add("productInfoRow")
			var globalCheckp =document.createElement("p");
			globalCheckp.appendChild(document.createTextNode("Is global:"))
			globalCheckp.setAttribute('style', 'font-size: 18px; width: 8%');
			globalCheckRow.appendChild(globalCheckp);
			var globalCheckInput =document.createElement("input");
			globalCheckInput.setAttribute('id', 'globalCheckInput');
			globalCheckInput.setAttribute('type', 'checkbox');
			globalCheckInput.checked=globalChecked;
			globalCheckInput.setAttribute("onchange", 'check()');
			globalCheckRow.appendChild(globalCheckInput);
			projectForm.appendChild(globalCheckRow);
    		
    		var changeLogoRow = document.createElement('div');
    		changeLogoRow.classList.add("productInfoRow");
    		changeLogoRow.setAttribute('style', 'resize: vertical;');
    		var changeLogoP = document.createElement('p');
    		changeLogoP.appendChild(document.createTextNode("Change logo: "));
    		changeLogoP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    		changeLogoRow.appendChild(changeLogoP);
    		var inputChangeLogo = document.createElement("input");
    		inputChangeLogo.setAttribute('id', 'projectLogo');
    		inputChangeLogo.setAttribute('type', 'file');
    		changeLogoRow.appendChild(inputChangeLogo);
    		projectForm.appendChild(changeLogoRow);

    		var saveBtnRow = document.createElement('div');
    		saveBtnRow.classList.add("productInfoRow");
    		saveBtnRow.setAttribute('style', 'justify-content: space-between');

			var milestonesBtn = document.createElement('button');
			milestonesBtn.classList.add("btn");
			milestonesBtn.classList.add("btn-primary");
			milestonesBtn.setAttribute("id", "milestonesBtn");
			milestonesBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
			milestonesBtn.appendChild(document.createTextNode("Show milestones"));
			milestonesBtn.onclick = function () {
				jQuery.ajax({
					url: "../api/milestones?prj=" + data.externalId,
					type: "GET",
					async: true,
					error: function(jqXHR, textStatus, errorThrown) {
						if (jqXHR.status == 500) warningUtils("Warning", "There is no information to show about milestones.");
					},
					success: function (milestones) {
						if (milestones.length > 0) {
							$("#milestonesItems").empty();
							milestones.forEach(function (milestone) {
								$("#milestonesItems").append('<tr class="milestoneItem"><td>' + milestone.date + '</td><td>' + milestone.type + '</td><td>' + milestone.name + '</td><td>' + milestone.description + '</td></tr>');
							});
							$("#milestonesModal").modal();
						} else warningUtils("Warning", "There is no information to show about milestones.");
					}
				})
			};
			saveBtnRow.appendChild(milestonesBtn);

			var phasesBtn = document.createElement('button');
			phasesBtn.classList.add("btn");
			phasesBtn.classList.add("btn-primary");
			phasesBtn.setAttribute("id", "phasesBtn");
			phasesBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
			phasesBtn.appendChild(document.createTextNode("Show phases"));
			phasesBtn.onclick = function () {
				jQuery.ajax({
					url: "../api/phases?prj=" + data.externalId,
					type: "GET",
					async: true,
					error: function(jqXHR, textStatus, errorThrown) {
						if (jqXHR.status == 500) warningUtils("Warning", "There is no information to show about phases.");
					},
					success: function (phases) {
						if (phases.length > 0) {
							$("#phasesItems").empty();
							phases.forEach(function (phase) {
								$("#phasesItems").append('<tr class="phaseItem"><td>' + phase.dateFrom + '</td><td>' + phase.dateTo + '</td><td>' + phase.name + '</td><td>' + phase.description + '</td></tr>');
							});
							$("#phasesModal").modal();
						} else warningUtils("Warning", "There is no information to show about phases.");
					}
				})
			};
			saveBtnRow.appendChild(phasesBtn);

    		var saveBtn = document.createElement('button');
    		saveBtn.classList.add("btn");
    		saveBtn.classList.add("btn-primary");
    		saveBtn.setAttribute("id", "saveBtn");
    		saveBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
    		saveBtn.appendChild(document.createTextNode("Save Project"));
    		saveBtn.onclick = saveProject;
    		saveBtnRow.appendChild(saveBtn);
			projectForm.appendChild(saveBtnRow);

			var divNames = document.createElement("div");
			divNames.classList.add("productInfoRow")
			var namesP = document.createElement('p');
			namesP.appendChild(document.createTextNode("Project Team Members"));
			namesP.setAttribute('style', 'font-size: 25px; margin-right: 1%');
			var divNames2 = document.createElement("div");
			divNames2.classList.add("productInfoRow")
			var namesExplanation = document.createElement('p');
			namesExplanation.appendChild(document.createTextNode("Press the icon with the pencil to assign metrics and save the student"));
			namesExplanation.setAttribute('style', 'font-size: 15px; margin-right: 1%');
			divNames.appendChild(namesP)
			divNames2.appendChild(namesExplanation)
			projectForm.appendChild(divNames);
			projectForm.appendChild(divNames2);
			var divFormNames= document.createElement("div");
			divFormNames.classList.add("productInfoRow");
			var tableRow = document.createElement('table');
			tableRow.classList.add("table");
			tableRow.setAttribute("id", "tableNames")
			var projetctr = document.createElement('tr');
			var projectbody = document.createElement('tbody');
			var thName=document.createElement('th');
			thName.appendChild(document.createTextNode("Name*"));
			thName.setAttribute("style", "width:16%")

			//LOAD TEAM MEMBERS HEAD IDENTITIES
			var thIdentities = []
			identities.forEach(identity => {
				let thIdentity = document.createElement('th');
				thIdentity.appendChild(document.createTextNode(identity + " username"));
				thIdentity.setAttribute("style", "width:18%");
				thIdentities.push(thIdentity);
			})

			var thMetric = document.createElement('th');
			thMetric.appendChild(document.createTextNode("Assign metrics and save student*"))
			thMetric.setAttribute("style", "width:15%;text-align: center")
			var thEmpty2 = document.createElement('th');
			thEmpty2.setAttribute("style", "width:2%")
			var thSpan=document.createElement('th');
			var Span = document.createElement('span');
			Span.setAttribute("class","table-addNames glyphicon glyphicon-plus");
			Span.setAttribute("style", "padding-left:40%;width:15%")
			Span.addEventListener("click", function()  {
				selectedStudent=tempId;
				//since Id can not be negative this would not have conflicts
				tempId=tempId-1;
				buildRow("","","","", selectedStudent);});
			thSpan.appendChild(Span);
			projetctr.appendChild(thName);
			thIdentities.forEach(thIdentity => {projetctr.appendChild(thIdentity);})
			projetctr.appendChild(thMetric);
			projetctr.appendChild(thEmpty2);
			projetctr.appendChild(thSpan);
			projectbody.appendChild(projetctr);
			tableRow.append(projectbody);
			divFormNames.append(tableRow);
			projectForm.appendChild(divFormNames);
    		
    		var logoColumn = document.createElement('div');
    		logoColumn.classList.add("logoColumn");
    		var logoP = document.createElement('img');
    		logoP.setAttribute("id", "logoP");
    		if (data.logo != null)
    			logoP.setAttribute('src', "data:image/png;base64," + data.logo);
    		logoP.setAttribute('style', 'max-width:96%; max-height:96%;');
    		logoColumn.appendChild(logoP);
    		
    		document.getElementById('productInfo').innerHTML = "";
    		document.getElementById('productInfo').appendChild(projectForm);
    		document.getElementById('productInfo').appendChild(logoColumn);
			for(let i = 0 ; i<data.students.length; i++) {
				var s = data.students[i];
				buildRow(s.name, s.identities, s.id);
			}

    		currentProject = currentProjectId;
        }
    });
}

function fieldEdited(studentName, identities, studentId) {
	var warning = document.getElementById("warning"+studentId);
	if(studentId<0 && warning!=null) {
		warning.hidden=false;
	}
	else {
		var name = document.getElementById("studentName" + studentId).innerHTML
		var change = name !== studentName;

		Object.values(identities).forEach(identity => {
		    var identity_name = document.getElementById("student"+identity.data_source+"Name" + studentId).innerHTML
		    if (identity_name !== identity.username){
		        change = false;
		    }
		});

		if(change){
		    warning.hidden=false
		}
        else{
            warning.hidden=true;
        }
	}

}

function buildRow(studentName, student_identities, studentId) {
	var table = document.getElementById("tableNames");
	var row = table.insertRow(-1);
	var name = document.createElement("td");
	name.setAttribute("contenteditable", "true");
	name.setAttribute("id" , "studentName" + studentId)
	name.setAttribute("style", "border:1px solid lightgray")
	name.addEventListener("input", function () {fieldEdited(studentName, student_identities, studentId)});
	name.innerHTML=studentName;
	row.appendChild(name);

	identities.forEach(identity => {
		var identityName = document.createElement("td");
		identityName.setAttribute("contenteditable", "true");
		identityName.setAttribute("style", "border:1px solid lightgray")
		identityName.setAttribute("id" , "student" + identity + "Name" + studentId)
		identityName.addEventListener("input", function () {fieldEdited(studentName, student_identities, studentId)});

		//CHECK IF username exists
		identityName.innerHTML = (student_identities[identity] !== undefined  && student_identities[identity].username !== undefined)
		                        ? student_identities[identity].username : "";
		row.appendChild(identityName);
	})

	var metricButton = document.createElement("th");
	//metricButton.setAttribute("style", "padding-left:5%")
	var warning = document.createElement("p")
	warning.appendChild(document.createTextNode("The student was not saved"));
	warning.setAttribute("style", "color:red;font-size: 12px;padding-left:2%;width:150%")
	warning.setAttribute("id", "warning"+studentId)
	if(studentId>0) warning.hidden=true;
	var selMetricsBtn = document.createElement('button');
	selMetricsBtn.classList.add("btn");
	selMetricsBtn.setAttribute('id', 'selMetricsBtn'+studentId);
	selMetricsBtn.setAttribute("style","margin-left:40%")
	var editIcon = document.createElement('img');
	editIcon.classList.add("icons");
	editIcon.src = '../icons/edit.png';
	selMetricsBtn.appendChild(editIcon);
	selMetricsBtn.addEventListener("click", function () {
		openMetricsModal(studentId)
	});
	metricButton.appendChild(selMetricsBtn);
	metricButton.appendChild(warning)
	row.appendChild(metricButton);

	var thEmpty = document.createElement('th');
	row.appendChild(thEmpty)

	var removeIcon = document.createElement("button");
	removeIcon.classList="btn btn-primary btn-danger"
	removeIcon.style="font-size: 15px;"
	removeIcon.appendChild(document.createTextNode("Delete Student"));
	//removeIcon.classList.add("glyphicon", "glyphicon-remove");
	var remove = document.createElement("th");
	remove.setAttribute("id", "remove" + studentId)
	remove.addEventListener("click", function () {
		deleteStudent(studentId);
	});

	remove.appendChild(removeIcon);

	row.appendChild(remove);
	row.setAttribute("id", "row" + studentId);
}

function deleteStudent(studentId) {


	if (studentId >= 0) {
		jQuery.ajax({
			url: `../api/metrics/students/${studentId}`,
			type: "DELETE",
			contentType: false,
			processData: false,
			success: function () {
				var delRow = document.getElementById("row" + studentId);
				delRow.remove()
				warningUtils("Ok", "Student deleted successfully");
			},
			error: function (jqXHR) {
				warningUtils("Error", "Datasource connection failed.");
			}
		});
	}
	else {
		var delRow = document.getElementById("row" + studentId);
		delRow.remove()
	}
}

function openMetricsModal(studentId) {
	selectedStudent=studentId
	showMetrics(studentId);
	var elem = document.getElementById("metricsModaldialog")
	elem.setAttribute("style", "width:900px");
	var elem2 = document.getElementById("metricRow")
	elem2.setAttribute("style", "width:100%")
	$("#metricsModal").modal();
};

$("#dismissMetricsButton").click(function () {
	$("#metricsModal").modal("hide");
});

$("#acceptMetricsButton").click(function () {

	var nameText = document.getElementById("studentName"+selectedStudent).innerText
	if(nameText==="") {
		$("#metricsModal").modal("hide");
		warningUtils("Warning", "The name is empty");
	}
	else {
		var userSelectedMetrics=[];
		$('#selMetricsBox').children().each (function (i, option) {
		    userSelectedMetrics.push(option.value);
		});

		var student_new_identities = {}
		identities.forEach(identity => {
		    var identity_new_value = document.getElementById(`student${identity}Name${selectedStudent}`).innerText;
		    if(identity_new_value === "") identity_new_value = null;
		    student_new_identities[identity] = identity_new_value;
		})

		var externalId =document.getElementById(currentSelectionId).innerHTML;
		var formData = new FormData();

		body = JSON.stringify({
		    "id": selectedStudent,
		    "name": nameText,
		    "identities": student_new_identities,
		    "metrics": userSelectedMetrics
		})

		jQuery.ajax({
			data: body,
			url: `../api/metrics/students?prj=${externalId}`,
			type: "PUT",
			contentType: "application/json;",
			processData: false,
			success: function (data) {
				$("#metricsModal").modal("hide");
				if(selectedStudent === "") {

					var row = document.getElementById("row" + selectedStudent)
					row.setAttribute("id", "row" + data);
					var name = document.getElementById("studentName" + selectedStudent)
					var nameClone = name.cloneNode(true)
					nameClone.setAttribute("id", "studentName" + data)
					nameClone.addEventListener("input",function() {fieldEdited(nameText, student_new_identities, data) })
					name.parentNode.replaceChild(nameClone,name)

                    identities.forEach(identity => {
                        var identityName = document.getElementById(`student${identity}Name` + selectedStudent)
                        var identityClone = identityName.cloneNode(true)
                        identityClone.setAttribute("id", `student${identity}Name` + data)
                        identityClone.addEventListener("input",function() {fieldEdited(nameText, student_new_identities, data) })
                        identityName.parentNode.replaceChild(identityClone, identityName)
                    })

					var selMetricsBtn = document.getElementById("selMetricsBtn"+selectedStudent)
					var selMetricsClone = selMetricsBtn.cloneNode(true)
					selMetricsClone.setAttribute("id", "selMetricsBtn"+data)
					selMetricsClone.addEventListener("click", function () {
						openMetricsModal(data)
					});
					selMetricsBtn.parentNode.replaceChild(selMetricsClone,selMetricsBtn)
					var warning = document.getElementById("warning"+selectedStudent);
					warning.hidden=true;
					warning.setAttribute("id", "warning" + data)
					var remove = document.getElementById("remove"+selectedStudent)
					var removeClone = remove.cloneNode(true)
					removeClone.setAttribute("id", "remove"+data)
					removeClone.addEventListener("click", function () {
						deleteStudent(data);
					});
					remove.parentNode.replaceChild(removeClone, remove)
					selectedStudent = data;
				}
				else {
					var warning = document.getElementById("warning"+selectedStudent);
					warning.hidden=true;
				}
				warningUtils("Ok", "Student saved successfully");
			},
			error: function(jqXHR) {
				$("#metricsModal").modal("hide");
				warningUtils("Error", "Datasource connection failed.");
			}
		});
	}



});

function updateSelectedMetrics() {
	for(var i=0; i<userSelectedMetrics.length; ++i) {
		if(userSelectedMetrics[i][0]==selectedStudent) {
			userSelectedMetrics.splice(i,1)
			i=i-1;
		}
	}
	$('#selMetricsBox').children().each (function (i, option) {
		userSelectedMetrics.push([selectedStudent, option.value]);
	});
	$("#metricsModal").modal("hide");
}

function moveMetricItemsLeft() {
	$('#selMetricsBox').find(':selected').appendTo('#avMetricsBox');
};

function moveAllMetricsItemsLeft() {
	$('#selMetricsBox').children().appendTo('#avMetricsBox');
};

function moveMetricItemsRight() {
	$('#avMetricsBox').find(':selected').appendTo('#selMetricsBox');
};

function moveAllMetricItemsRight() {
	$('#avMetricsBox').children().appendTo('#selMetricsBox');
};

function showMetrics(studentId) {
	var externalId =document.getElementById(currentSelectionId).innerHTML;
	jQuery.ajax({
		dataType: "json",
		url: `../api/metrics?prj=${externalId}`,
		cache: false,
		type: "GET",
		async: false,
		success: function (data) {
			$('#avMetricsBox').empty();
			$('#selMetricsBox').empty();
			data.forEach(function (metric) {
				if(metric.student==null) {
					$('#avMetricsBox').append($('<option>', {
						value: metric.id,
						text: metric.name
					}));
				}
				if(metric.student!=null && studentId !== undefined && metric.student.id===studentId) {
					$('#avMetricsBox').append($('<option>', {
						value: metric.id,
						text: metric.name
					}));
					$('#avMetricsBox').find("option[value='" + metric.id + "']").appendTo('#selMetricsBox');
				}

			});
		},
		error: function(jqXHR) {
			warningUtils("Error", "Datasource connection failed.");
			$("#metricsModal").modal("hide");
		}
	});
}

function check() {
	if(globalChecked) globalChecked=false
	else globalChecked=true
}



function saveProject() {

	if ($('#projectName').val() != "") {
    	var loadedFile = $('#projectLogo')[0].files[0];
    	if (loadedFile == null || loadedFile.size < 1048576) {
	        var formData = new FormData();

	        var project_new_external_id = document.getElementById("projectId").innerHTML;
            var project_new_name = $('#projectName').val();
	        var description = $('#projectDescription').val()

	        if (description === "") {
	            description="No description specified";
	        }

            var project_new_logo = $('#projectLogo')[0].files[0];
	        var backlogId = $("#projectBacklogId").val();

	        if (backlogId === "") {
	            backlogId=null;
	        }

	        var url = "/api/projects/" + currentProject;
			if (serverUrl) {
				url = serverUrl + url;
			}

            project_new_identities = {};

            identities.forEach(identity => {
                var identity_new_value = document.getElementById(`input${identity}Url`).value;
                project_new_identities[identity] = identity_new_value;
            })

            var externalId =document.getElementById(currentSelectionId).innerHTML;
            var formData = new FormData();

            body = JSON.stringify({
                "external_id": project_new_external_id,
                "name": project_new_name,
                "description": description,
                "identities": project_new_identities,
                "global": globalChecked,
                "backlog_id": backlogId
            })

            var data = new FormData();
            data.append("data", new Blob([body], {type: "application/json"}));
            data.append("file", project_new_logo);

	        $.ajax({
	            url: url,
			    data: data,
			    contentType: false,
	            type: "PUT",
	            processData: false,
	            error: function(jqXHR, textStatus, errorThrown) {
	                if (jqXHR.status == 409)
	                	warningUtils("Error", "This Project name is already in use");
	                else {
						warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
	                    location.href = "../Products/Configuration";
	                }
	            },
	            success: function() {
	            	/*buildFirstPartOfTree();
	            	getChosenProject(currentProjectId);*/
	            	location.href = serverUrl + "/Products/Configuration";
	            }
	        });
    	} else {
    		warningUtils("Error", "The logo exceeds its maximum permitted size of 1Mb.");
        }
    } else warningUtils("Warning", "Make sure that you have completed all fields marked with an *");
};

function getChosenProduct(currentProductId) {
	var url = "/api/products/" + currentProductId;
	if (serverUrl) {
		url = serverUrl + url;
	}
    jQuery.ajax({
        dataType: "json",
        url: url,
        cache: false,
        type: "GET",
        async: true,
        success: function (data) {
        	var productForm = document.createElement('div');
    		productForm.setAttribute("id", "productForm");
    		
    		var title1Row = document.createElement('div');
    		title1Row.classList.add("productInfoRow");
    		var title1P = document.createElement('p');
    		title1P.appendChild(document.createTextNode("Product Information"));
    		title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    		title1Row.appendChild(title1P);
    		productForm.appendChild(title1Row);
    		
    		var nameRow = document.createElement('div');
    		nameRow.classList.add("productInfoRow");
    		var nameP = document.createElement('p');
        	nameP.appendChild(document.createTextNode("Name*: "));
        	nameP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
        	nameRow.appendChild(nameP);
    		var inputName = document.createElement("input");
    		inputName.setAttribute('id', 'productName');
    		inputName.setAttribute('type', 'text');
    		inputName.setAttribute('value', data.name);
    		inputName.setAttribute('style', 'width: 100%;');
			inputName.setAttribute('placeholder', 'Write the product name here');
    		nameRow.appendChild(inputName);
    		productForm.appendChild(nameRow);
    		
    		var descriptionRow = document.createElement('div');
    		descriptionRow.classList.add("productInfoRow");
    		descriptionRow.setAttribute('style', 'resize: vertical;');
    		var descriptionP = document.createElement('p');
    		descriptionP.appendChild(document.createTextNode("Description: "));
    		descriptionP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    		descriptionRow.appendChild(descriptionP);
    		var inputDescription = document.createElement("textarea");
    		inputDescription.setAttribute('id', 'productDescription');
    		inputDescription.value= data.description;
    		inputDescription.setAttribute('style', 'width: 100%;');
    		inputDescription.setAttribute('rows', '3');
			inputDescription.setAttribute('placeholder', 'Write the product description here');
    		descriptionRow.appendChild(inputDescription);
    		productForm.appendChild(descriptionRow);
    		
    		var changeLogoRow = document.createElement('div');
    		changeLogoRow.classList.add("productInfoRow");
    		changeLogoRow.setAttribute('style', 'resize: vertical;');
    		var changeLogoP = document.createElement('p');
    		changeLogoP.appendChild(document.createTextNode("Change logo: "));
    		changeLogoP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    		changeLogoRow.appendChild(changeLogoP);
    		var inputChangeLogo = document.createElement("input");
    		inputChangeLogo.setAttribute('id', 'productLogo');
    		inputChangeLogo.setAttribute('type', 'file');
    		changeLogoRow.appendChild(inputChangeLogo);
    		productForm.appendChild(changeLogoRow);
    		
    		var title2Row = document.createElement('div');
    		title2Row.classList.add("productInfoRow");
    		var title2P = document.createElement('p');
    		title2P.appendChild(document.createTextNode("Product Composition"));
    		title2P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    		title2Row.appendChild(title2P);
    		productForm.appendChild(title2Row);
    		
    		var projectsRow = document.createElement('div');
    		projectsRow.classList.add("productInfoRow");
    		var selProjectsCol = document.createElement('div');
    		selProjectsCol.classList.add("selectionColumn");
    		selProjectsCol.setAttribute('style', 'width: 100%');
    		var selProjectsP = document.createElement('p');
    		selProjectsP.appendChild(document.createTextNode("Selected Projects*: "));
    		selProjectsP.setAttribute('style', 'font-size: 18px; margin-bottom: 1%');
    		selProjectsCol.appendChild(selProjectsP);
    		var selProjectsBox = document.createElement('select');
    		selProjectsBox.setAttribute('id', 'selProjectsBox');
    		selProjectsBox.setAttribute('multiple', 'multiple');
    		selProjectsBox.setAttribute('style', 'height: 150px;');
    		var projectsNames = [];
    		for (var i = 0; i < data.projects.length; i++) {
    			var opt = document.createElement("option");
    			opt.value = data.projects[i].id;
    			opt.innerHTML = data.projects[i].name;
    			selProjectsBox.appendChild(opt);
    			projectsNames.push(data.projects[i].name);
    		}
    		selProjectsCol.appendChild(selProjectsBox);
    		var avProjectsCol = document.createElement('div');
    		avProjectsCol.classList.add("selectionColumn");
    		avProjectsCol.setAttribute('style', 'width: 100%');
    		var avProjectsP = document.createElement('p');
    		avProjectsP.appendChild(document.createTextNode("Available Projects: "));
    		avProjectsP.setAttribute('style', 'font-size: 18px; margin-bottom: 1%');
    		avProjectsCol.appendChild(avProjectsP);
    		var avProjectsBox = document.createElement('select');
    		avProjectsBox.setAttribute('id', 'avProjectsBox');
    		avProjectsBox.setAttribute('multiple', 'multiple');
    		avProjectsBox.setAttribute('style', 'height: 150px;');
    		for (var i = 0; i < projects.length; i++) {
    			if(!projectsNames.includes(projects[i].name)) {
    				var opt = document.createElement("option");
        			opt.setAttribute('id', ('opt' + projects[i].name));
        			opt.value = projects[i].id;
        			opt.innerHTML = projects[i].name;
        			avProjectsBox.appendChild(opt);
    			}
    		}
    		avProjectsCol.appendChild(avProjectsBox);
	        var arrowsCol = document.createElement('div');
	        arrowsCol.classList.add("selectionColumn");
	        arrowsCol.setAttribute('style', 'padding-top:30px;');
	        var arrowLeft = document.createElement('button');
	        arrowLeft.classList.add("btn");
	        arrowLeft.classList.add("btn-default");
	        arrowLeft.classList.add("top-and-bottom-margin");
	        arrowLeft.setAttribute('id', 'oneLeft');
	        arrowLeft.appendChild(document.createTextNode("<"));
	        arrowLeft.onclick = moveItemsLeft;
	        arrowsCol.appendChild(arrowLeft);
	        var arrowRight = document.createElement('button');
	        arrowRight.classList.add("btn");
	        arrowRight.classList.add("btn-default");
	        arrowRight.classList.add("top-and-bottom-margin");
	        arrowRight.setAttribute('id', 'right');
	        arrowRight.appendChild(document.createTextNode(">"));
	        arrowRight.onclick = moveItemsRight;
	        arrowRight.setAttribute('style', "margin-top:3px;");
	        arrowsCol.appendChild(arrowRight);
	        var arrowAllRight = document.createElement('button');
	        arrowAllRight.classList.add("btn");
	        arrowAllRight.classList.add("btn-default");
	        arrowAllRight.classList.add("top-and-bottom-margin");
	        arrowAllRight.setAttribute('id', 'allRight');
	        arrowAllRight.appendChild(document.createTextNode(">>"));
	        arrowAllRight.onclick = moveAllItemsRight;
	        arrowAllRight.setAttribute('style', "margin-top:3px;");
	        arrowsCol.appendChild(arrowAllRight);
	        var arrowAllLeft = document.createElement('button');
	        arrowAllLeft.classList.add("btn");
	        arrowAllLeft.classList.add("btn-default");
	        arrowAllLeft.classList.add("top-and-bottom-margin");
	        arrowAllLeft.setAttribute('id', 'allLeft');
	        arrowAllLeft.appendChild(document.createTextNode("<<"));
	        arrowAllLeft.onclick = moveAllItemsLeft;
	        arrowAllLeft.setAttribute('style', "margin-top:3px;");
	        arrowsCol.appendChild(arrowAllLeft);
	        
    		projectsRow.appendChild(avProjectsCol);
    		projectsRow.appendChild(arrowsCol);
    		projectsRow.appendChild(selProjectsCol);
    		productForm.appendChild(projectsRow);
    		
    		var saveBtnRow = document.createElement('div');
    		saveBtnRow.classList.add("productInfoRow");
    		saveBtnRow.setAttribute('style', 'justify-content: space-between');
    		var deleteBtn = document.createElement('button');
    		deleteBtn.classList.add("btn");
    		deleteBtn.classList.add("btn-primary");
    		deleteBtn.classList.add("btn-danger");
    		deleteBtn.setAttribute("id", "deleteBtn");
    		deleteBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
    		deleteBtn.appendChild(document.createTextNode("Delete Product"));
    		deleteBtn.onclick = deleteProduct;
    		saveBtnRow.appendChild(deleteBtn);
    		var saveBtn = document.createElement('button');
    		saveBtn.classList.add("btn");
    		saveBtn.classList.add("btn-primary");
    		saveBtn.setAttribute("id", "saveBtn");
    		saveBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
    		saveBtn.appendChild(document.createTextNode("Save Product"));
    		saveBtn.onclick = saveProduct;
    		saveBtnRow.appendChild(saveBtn);
    		productForm.appendChild(saveBtnRow);
    		
    		var logoColumn = document.createElement('div');
    		logoColumn.classList.add("logoColumn");
    		var logoContainer = document.createElement('div');
    		logoContainer.classList.add("logoContainer");
    		var logoP = document.createElement('img');
    		logoP.setAttribute("id", "logoP");
    		if (data.logo != null)
    			logoP.setAttribute('src', "data:image/png;base64," + data.logo);
    		logoP.setAttribute('style', 'max-width:96%; max-height:96%;');
    		logoContainer.appendChild(logoP);
    		logoColumn.appendChild(logoContainer);
    		
    		document.getElementById('productInfo').innerHTML = "";
    		document.getElementById('productInfo').appendChild(productForm);
    		document.getElementById('productInfo').appendChild(logoColumn);
    		
    		currentProduct = currentProductId;
        }
    });
}



function moveItemsLeft() {
    $('#selProjectsBox').find(':selected').appendTo('#avProjectsBox');
};

function moveAllItemsLeft() {
    $('#selProjectsBox').children().appendTo('#avProjectsBox');
};

function moveItemsRight() {
    $('#avProjectsBox').find(':selected').appendTo('#selProjectsBox');
};

function moveAllItemsRight() {
	$('#avProjectsBox').children().appendTo('#selProjectsBox');
};

function saveProduct() {
	var selectedProjects = [];

    $('#selProjectsBox').children().each (function (i, option) {
    	selectedProjects.push(option.value);
    });

    if ($('#productName').val() != "" && selectedProjects.length > 0) {
    	var loadedFile = $('#productLogo')[0].files[0];
    	if (loadedFile == null || loadedFile.size < 1048576) {
    		var formData = new FormData();
            formData.append("name", $('#productName').val());
            formData.append("description", $('#productDescription').val());
            formData.append("logo", $('#productLogo')[0].files[0]);
            formData.append("projects", selectedProjects);

            var url = "/api/products/" + currentProduct;
			if (serverUrl) {
				url = serverUrl + url;
			}
            
            $.ajax({
                url: url,
                data: formData,
                type: "PUT",
                contentType: false,
                processData: false,
                error: function(jqXHR, textStatus, errorThrown) {
                    if (jqXHR.status == 409)
						warningUtils("Error", "This Product name is already in use");
                    else {
						warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
                        location.href = serverUrl + "/Products/Configuration";
                    }
                },
                success: function() {
                	buildFirstPartOfTree();
                	getChosenProduct(currentProduct);
                }
            });
        } else {
			warningUtils("Error", "The logo exceeds its maximum permitted size of 1Mb.");
        } 
    } else warningUtils("Warning", "Make sure that you have completed all fields marked with an *");
};

function deleteProduct() {
	if (confirm("Are you sure you want to delete this product?")) {

        var url = "/api/products/" + currentProduct;
		if (serverUrl) {
			url = serverUrl + url;
		}
        $.ajax({
            url: url,
            type: "DELETE",
            contentType: false,
            processData: false,
            error: function(jqXHR, textStatus, errorThrown) {
				warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
                location.href = serverUrl + "/Products/Configuration";
            },
            success: function() {
            	buildFirstPartOfTree();
            	checkProducts();
            	document.getElementById('productInfo').innerHTML = "";
            }
        });
	}
};

function newProduct() {
	var productForm = document.createElement('div');
	productForm.setAttribute("id", "productForm");
	
	var title1Row = document.createElement('div');
	title1Row.classList.add("productInfoRow");
	var title1P = document.createElement('p');
	title1P.appendChild(document.createTextNode("Step 1 - Fill your product information"));
	title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
	title1Row.appendChild(title1P);
	productForm.appendChild(title1Row);
	
	var nameRow = document.createElement('div');
	nameRow.classList.add("productInfoRow");
	var nameP = document.createElement('p');
	nameP.appendChild(document.createTextNode("Name*: "));
	nameP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
	nameRow.appendChild(nameP);
	var inputName = document.createElement("input");
	inputName.setAttribute('id', 'productName');
	inputName.value = "";
	inputName.setAttribute('rows', '1');
	inputName.setAttribute('style', 'width: 100%;');
	inputName.setAttribute('placeholder', 'Write the product name here');
	nameRow.appendChild(inputName);
	productForm.appendChild(nameRow);
	
	var descriptionRow = document.createElement('div');
	descriptionRow.classList.add("productInfoRow");
	descriptionRow.setAttribute('style', 'resize: vertical;');
	var descriptionP = document.createElement('p');
	descriptionP.appendChild(document.createTextNode("Description: "));
	descriptionP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
	descriptionRow.appendChild(descriptionP);
	var inputDescription = document.createElement("textarea");
	inputDescription.setAttribute('id', 'productDescription');
	inputDescription.value= "";
	inputDescription.setAttribute('style', 'width: 100%;');
	inputDescription.setAttribute('rows', '3');
	inputDescription.setAttribute('placeholder', 'Write the product description here');
	descriptionRow.appendChild(inputDescription);
	productForm.appendChild(descriptionRow);
	
	var changeLogoRow = document.createElement('div');
	changeLogoRow.classList.add("productInfoRow");
	changeLogoRow.setAttribute('style', 'resize: vertical;');
	var changeLogoP = document.createElement('p');
	changeLogoP.appendChild(document.createTextNode("Choose logo: "));
	changeLogoP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
	changeLogoRow.appendChild(changeLogoP);
	var inputChangeLogo = document.createElement("input");
	inputChangeLogo.setAttribute('id', 'newProductLogo');
	inputChangeLogo.setAttribute('type', 'file');
	changeLogoRow.appendChild(inputChangeLogo);
	productForm.appendChild(changeLogoRow);
	
	var title2Row = document.createElement('div');
	title2Row.classList.add("productInfoRow");
	var title2P = document.createElement('p');
	title2P.appendChild(document.createTextNode("Step 2 - Select the corresponding projects"));
	title2P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
	title2Row.appendChild(title2P);
	productForm.appendChild(title2Row);
	
	var selProjectsCol = document.createElement('div');
	selProjectsCol.classList.add("selectionColumn");
	selProjectsCol.setAttribute('style', 'width: 100%');
	var selProjectsP = document.createElement('p');
	selProjectsP.appendChild(document.createTextNode("Selected Projects*: "));
	selProjectsP.setAttribute('style', 'font-size: 18px; margin-bottom: 1%');
	selProjectsCol.appendChild(selProjectsP);
	var selProjectsBox = document.createElement('select');
	selProjectsBox.setAttribute('id', 'selProjectsBox');
	selProjectsBox.setAttribute('multiple', 'multiple');
	selProjectsBox.setAttribute('style', 'height: 150px;');
	selProjectsCol.appendChild(selProjectsBox);
	var projectsRow = document.createElement('div');
	projectsRow.classList.add("productInfoRow");
	var avProjectsCol = document.createElement('div');
	avProjectsCol.classList.add("selectionColumn");
	avProjectsCol.setAttribute('style', 'width: 100%');
	var avProjectsP = document.createElement('p');
	avProjectsP.appendChild(document.createTextNode("Available Projects: "));
	avProjectsP.setAttribute('style', 'font-size: 18px; margin-bottom: 1%');
	avProjectsCol.appendChild(avProjectsP);
	var avProjectsBox = document.createElement('select');
	avProjectsBox.setAttribute('id', 'avProjectsBox');
	avProjectsBox.setAttribute('multiple', 'multiple');
	avProjectsBox.setAttribute('style', 'height: 150px;');
	for (var i = 0; i < projects.length; i++) {
		var opt = document.createElement("option");
		opt.setAttribute('id', ('opt' + projects[i].name));
		opt.value = projects[i].id;
		opt.innerHTML = projects[i].name;
		avProjectsBox.appendChild(opt);
	}
	avProjectsCol.appendChild(avProjectsBox);
    var arrowsCol = document.createElement('div');
    arrowsCol.classList.add("selectionColumn");
    arrowsCol.setAttribute('style', 'padding-top:30px;');
    var arrowLeft = document.createElement('button');
    arrowLeft.classList.add("btn");
    arrowLeft.classList.add("btn-default");
    arrowLeft.classList.add("top-and-bottom-margin");
    arrowLeft.setAttribute('id', 'oneLeft');
    arrowLeft.appendChild(document.createTextNode("<"));
    arrowLeft.onclick = moveItemsLeft;
    arrowsCol.appendChild(arrowLeft);
    var arrowRight = document.createElement('button');
    arrowRight.classList.add("btn");
    arrowRight.classList.add("btn-default");
    arrowRight.classList.add("top-and-bottom-margin");
    arrowRight.setAttribute('id', 'right');
    arrowRight.appendChild(document.createTextNode(">"));
    arrowRight.onclick = moveItemsRight;
    arrowRight.setAttribute('style', "margin-top:3px;");
    arrowsCol.appendChild(arrowRight);
    var arrowAllRight = document.createElement('button');
    arrowAllRight.classList.add("btn");
    arrowAllRight.classList.add("btn-default");
    arrowAllRight.classList.add("top-and-bottom-margin");
    arrowAllRight.setAttribute('id', 'allRight');
    arrowAllRight.appendChild(document.createTextNode(">>"));
    arrowAllRight.onclick = moveAllItemsRight;
    arrowAllRight.setAttribute('style', "margin-top:3px;");
    arrowsCol.appendChild(arrowAllRight);
    var arrowAllLeft = document.createElement('button');
    arrowAllLeft.classList.add("btn");
    arrowAllLeft.classList.add("btn-default");
    arrowAllLeft.classList.add("top-and-bottom-margin");
    arrowAllLeft.setAttribute('id', 'allLeft');
    arrowAllLeft.appendChild(document.createTextNode("<<"));
    arrowAllLeft.onclick = moveAllItemsLeft;
    arrowAllLeft.setAttribute('style', "margin-top:3px;");
    arrowsCol.appendChild(arrowAllLeft);
    
	projectsRow.appendChild(avProjectsCol);
	projectsRow.appendChild(arrowsCol);
	projectsRow.appendChild(selProjectsCol);
	productForm.appendChild(projectsRow);
	
	
	var saveBtnRow = document.createElement('div');
	saveBtnRow.classList.add("productInfoRow");
	saveBtnRow.setAttribute('style', 'justify-content: flex-end');
	var saveBtn = document.createElement('button');
	saveBtn.classList.add("btn");
	saveBtn.classList.add("btn-primary");
	saveBtn.setAttribute("id", "saveBtn");
	saveBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
	saveBtn.appendChild(document.createTextNode("Save Product"));
	saveBtn.onclick = saveNewProduct;
	saveBtnRow.appendChild(saveBtn);
	productForm.appendChild(saveBtnRow);
	
	document.getElementById('productInfo').innerHTML = "";
	document.getElementById('productInfo').appendChild(productForm);
}

function saveNewProduct() {
	var selectedProjects = [];

    $('#selProjectsBox').children().each (function (i, option) {
    	selectedProjects.push(option.value);
    });

    if ($('#productName').val() != "" && selectedProjects.length > 0) {
    	var loadedFile = $('#newProductLogo')[0].files[0];
    	if (loadedFile == null || loadedFile.size < 1048576) {
    		var formData = new FormData();
            formData.append("name", $('#productName').val());
            formData.append("description", $('#productDescription').val());
            formData.append("logo", $('#newProductLogo')[0].files[0]);
            formData.append("projects", selectedProjects);

			var url = "/api/products";
			if (serverUrl) {
				url = serverUrl + url;
			}

            $.ajax({
                url: url,
                data: formData,
                type: "POST",
                contentType: false,
                processData: false,
                error: function(jqXHR, textStatus, errorThrown) {
                    if (jqXHR.status == 409)
                    	warningUtils("Error", "This Product name is already in use");
                    else {
						warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
                        location.href = serverUrl + "/Products/Configuration";
                    }
                },
                success: function() {
                	buildFirstPartOfTree();
                	checkProducts();
                	document.getElementById('productInfo').innerHTML = "";
                }
            });
        } else {
			warningUtils("Error", "The logo exceeds its maximum permitted size of 1Mb.");
        } 
    } else warningUtils("Warning", "Make sure that you have completed all fields marked with an *");
};

function goToDetailedEvaluation() {
	location.href = serverUrl + "/Products/DetailedEvaluation";
}

function goToEvaluation() {
	location.href = serverUrl + "/Products/Evaluation";
}

window.onload = function() {
	buildFirstPartOfTree();
	loadIdentities();
};


