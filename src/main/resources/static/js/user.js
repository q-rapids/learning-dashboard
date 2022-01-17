var userProjects = [];
var notUserProjects = [];
var allProjects = [];
var listOfAllowedProjects = [];

function showProjects () {
    $('#avProjectsBox').empty();
    $('#selProjectsBox').empty();
    notUserProjects.forEach(function (project) {
        $('#avProjectsBox').append($('<option>', {
            value: project.id,
            text: project.name
        }));
    });
    userProjects.forEach(function (project) {
        $('#selProjectsBox').append($('<option>', {
            value: project.id,
            text: project.name
        }));
    });
}
var serverUrl = sessionStorage.getItem("serverUrl");

function loadProjects () {
    // get metrics from DB
    console.log("ARRIBA");

    $.ajax({
        url: serverUrl + "/api/projects",
        type: "GET",
        async: true,
        success: function(data) {
            data.forEach(function (project) {
                allProjects.push(project);
            });
            getActiveUserProjects();
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

function getCookie(cname) {
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for(let i = 0; i <ca.length; i++) {
        let c = ca[i];
        while (c.charAt(0) == ' ') {
            c = c.substring(1);
        }
        if (c.indexOf(name) == 0) {
            return c.substring(name.length, c.length);
        }
    }
    return "";
}

function getActiveUserProjects() {

    token = getCookie("xFOEto4jYAjdMeR3Pas6_");

    var id=document.getElementById("userEdited").value;

    if(token!="") {
        jQuery.ajax({
            dataType: "json",
            url: serverUrl+"/api/allowedprojects?token="+token + "&id=" + id,
            cache: false,
            type: "GET",
            async: false,
            success: function (data) {
                allProjects.forEach(function (prj) {
                    if(data.includes(prj.externalId)) {
                        userProjects.push(prj);
                    }
                    else notUserProjects.push(prj);
                });
                showProjects();
            },
            error: function() {
                console.log("ERROR");
            }
        });
    }

}

function updateProjects() {

    var prj = [];
    $('#selProjectsBox').children().each (function (i, option) {
        prj.push(option.value);
    });

    var id=document.getElementById("userEdited").value;

    jQuery.ajax({
        dataType: "json",
        url: serverUrl+"/api/allowedprojects?id=" + id,
        cache: false,
        type: "PUT",
        async: false,
        data: JSON.stringify(prj),
        contentType: "application/json",
        success: function () {
        },
        error: function() {

        }
    });


}


loadProjects();


