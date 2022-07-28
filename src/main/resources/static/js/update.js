var currentProject;
var currentProduct;
var previousSelectionId;
var currentSelectionId;
var updates;
var areProducts = false;
var serverUrl = sessionStorage.getItem("serverUrl");


function buildFirstPartOfTree() {
    var url = "/api/update";
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
            var updateList = document.getElementById('productList');
            updateList.innerHTML = "";
            var update = document.createElement('li');
            update.classList.add("list-group-item");
            var updateP = document.createElement('p');
            updateP.setAttribute("data-toggle", "collapse");
            updateP.setAttribute("data-target", "#projectList");
            updateP.appendChild(document.createTextNode("All Updates"));
            update.appendChild(updateP);
            updateList.appendChild(update);

            var updateinsideList = document.createElement('ul');
            updateinsideList.classList.add("collapse");
            updateinsideList.setAttribute("id", "projectList");

            for (var i = 0; i < data.length; i++) {
                var update = document.createElement('li');
                update.classList.add("list-group-item");
                update.classList.add("Update");
                update.appendChild(document.createTextNode(data[i].name));
                update.setAttribute("id", ("update" + data[i].id));
                update.addEventListener("click", clickOnTree);
                updateinsideList.appendChild(update);
            }
            updateList.appendChild(updateinsideList);
            updates = data;
        }
    });
};


function clickOnTree(e){
    console.log(currentSelectionId);
    previousSelectionId = currentSelectionId;
    currentSelectionId = e.target.id;
    if (previousSelectionId != null) {
        document.getElementById(previousSelectionId).setAttribute('style', 'background-color: #ffffff;');
    }
    document.getElementById(currentSelectionId).setAttribute('style', 'background-color: #efeff8;');
    var idString = e.target.id.split("-")[0];
    console.log(idString.replace("update", ""));
    getChosenUpdate(idString.replace("update", ""));

}

function getChosenUpdate(currentProductId) {
    var url = "/api/update/" + currentProductId;
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
            title1P.appendChild(document.createTextNode("Update Information"));
            title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
            title1Row.appendChild(title1P);
            productForm.appendChild(title1Row);

            var dateRow = document.createElement('div');
            dateRow.classList.add("productInfoRow");
            var dateP = document.createElement('p');
            dateP.appendChild(document.createTextNode("Date: "));
            dateP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
            dateRow.appendChild(dateP);
            var dateInput =  document.createElement('p');
            dateInput.setAttribute('style', 'font-size: 18px; margin-right: 1%');
            dateInput.setAttribute('id', 'productDate');
            let today = new Date().toISOString().slice(0, 10)
            dateInput.innerHTML=data.date;
            dateRow.appendChild(dateInput)
            productForm.appendChild(dateRow)

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
            inputName.setAttribute('placeholder', 'Write the update name here');
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
            inputDescription.value= data.update;
            inputDescription.setAttribute('style', 'width: 100%;');
            inputDescription.setAttribute('rows', '3');
            inputDescription.setAttribute('placeholder', 'Write the update description here');
            descriptionRow.appendChild(inputDescription);
            productForm.appendChild(descriptionRow);

            var saveBtnRow = document.createElement('div');
            saveBtnRow.classList.add("productInfoRow");
            saveBtnRow.setAttribute('style', 'justify-content: space-between');
            var deleteBtn = document.createElement('button');
            deleteBtn.classList.add("btn");
            deleteBtn.classList.add("btn-primary");
            deleteBtn.classList.add("btn-danger");
            deleteBtn.setAttribute("id", "deleteBtn");
            deleteBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
            deleteBtn.appendChild(document.createTextNode("Delete Update"));
            deleteBtn.onclick = deleteProduct;
            saveBtnRow.appendChild(deleteBtn);
            var saveBtn = document.createElement('button');
            saveBtn.classList.add("btn");
            saveBtn.classList.add("btn-primary");
            saveBtn.setAttribute("id", "saveBtn");
            saveBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
            saveBtn.appendChild(document.createTextNode("Save Update"));
            saveBtn.onclick = saveProduct;
            saveBtnRow.appendChild(saveBtn);
            productForm.appendChild(saveBtnRow);

            document.getElementById('productInfo').innerHTML = "";
            document.getElementById('productInfo').appendChild(productForm);

            currentProduct = currentProductId;
        }
    });
}

function saveProduct() {

    if ($('#productName').val() != "" ) {
        var date = document.getElementById("productDate").innerHTML
        var formData = new FormData();
        formData.append("name", $('#productName').val());
        formData.append("update", $('#productDescription').val());
        formData.append("date",date);

            var url = "/api/update/" + currentProduct;
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
                    warningUtils("Error", "Error while saving the Update");
                },
                success: function() {
                    warningUtils("Ok", "Update saved successfully");
                    buildFirstPartOfTree();
                    getChosenUpdate(currentProduct);
                }
            });
    } else warningUtils("Warning", "Make sure that you have completed all fields marked with an *");
};

function deleteProduct() {

    var url = "/api/update/" + currentProduct;
    if (serverUrl) {
        url = serverUrl + url;
    }
    $.ajax({
        url: url,
        type: "DELETE",
        contentType: false,
        processData: false,
        error: function(jqXHR, textStatus, errorThrown) {
            warningUtils("Error", "Error while deleting update");
        },
        success: function() {
            warningUtils("Ok", "Update deleted successfully");
            buildFirstPartOfTree();
            currentSelectionId=null;
            document.getElementById('productInfo').innerHTML = "";
        }
    });

};

function newUpdate() {
    var productForm = document.createElement('div');
    productForm.setAttribute("id", "productForm");

    var title1Row = document.createElement('div');
    title1Row.classList.add("productInfoRow");
    var title1P = document.createElement('p');
    title1P.appendChild(document.createTextNode("Step 1 - Fill your update information"));
    title1P.setAttribute('style', 'font-size: 36px; margin-right: 1%');
    title1Row.appendChild(title1P);
    productForm.appendChild(title1Row);

    var dateRow = document.createElement('div');
    dateRow.classList.add("productInfoRow");
    var dateP = document.createElement('p');
    dateP.appendChild(document.createTextNode("Date: "));
    dateP.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    dateRow.appendChild(dateP);
    var dateInput =  document.createElement('p');
    dateInput.setAttribute('style', 'font-size: 18px; margin-right: 1%');
    dateInput.setAttribute('id', 'productDate');
    let today = new Date().toISOString().slice(0, 10)
    dateInput.innerHTML=today;
    dateRow.appendChild(dateInput)
    productForm.appendChild(dateRow)

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
    inputName.setAttribute('placeholder', 'Write the update name here');
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
    inputDescription.setAttribute('placeholder', 'Write the update description here');
    descriptionRow.appendChild(inputDescription);
    productForm.appendChild(descriptionRow);


    var saveBtnRow = document.createElement('div');
    saveBtnRow.classList.add("productInfoRow");
    saveBtnRow.setAttribute('style', 'justify-content: flex-end');
    var saveBtn = document.createElement('button');
    saveBtn.classList.add("btn");
    saveBtn.classList.add("btn-primary");
    saveBtn.setAttribute("id", "saveBtn");
    saveBtn.setAttribute('style', 'font-size: 18px; max-width: 30%;');
    saveBtn.appendChild(document.createTextNode("Save Update"));
    saveBtn.onclick = saveNewUpdate;
    saveBtnRow.appendChild(saveBtn);
    productForm.appendChild(saveBtnRow);

    document.getElementById('productInfo').innerHTML = "";
    document.getElementById('productInfo').appendChild(productForm);
}

function saveNewUpdate() {

    if ($('#productName').val() != "") {
            var date = document.getElementById("productDate").innerHTML
            var formData = new FormData();
            formData.append("name", $('#productName').val());
            formData.append("update", $('#productDescription').val());
            formData.append("date",date);

            var url = "/api/update";
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
                    warningUtils("Error", "Error while saving the Update");
                },
                success: function() {
                    buildFirstPartOfTree();
                    document.getElementById('productInfo').innerHTML = "";
                    warningUtils("Ok", "Update saved successfully");
                }
            });
        }
    else warningUtils("Warning", "Make sure that you have completed all fields marked with an *");
};

window.onload = function() {
    buildFirstPartOfTree();
};