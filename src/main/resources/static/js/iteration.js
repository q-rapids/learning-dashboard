let iterations = [];
let projects = [];
let currentIterationId;
let httpMethod;

function getProjects() {
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
            projects = data;
        }
    });
}

function buildIterationList() {
    var url = "/api/project/historicdates";
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
            iterations = data;
            var iterationList = document.getElementById('iterationList');
            for (var i = 0; i < data.length; i++) {
                let iteration = document.createElement('li');
                iteration.classList.add("list-group-item");
                iteration.classList.add("iteration");
                iteration.setAttribute("id", (data[i].id));
                iteration.appendChild(document.createTextNode(getIterationName(data[i])));
                iteration.addEventListener("click", clickOnTree);
                iterationList.appendChild(iteration);
            }
            document.getElementById('iterationTree').appendChild(iterationList);
        }
    });
}

function getIterationName(iteration){
    if(iteration.label === "")
        return iteration.name
    else
        return iteration.name + ' (' + iteration.label + ')'
}

function newIteration() {
    httpMethod = 'POST';

    $('#iterationInfo').show();
    $('#iterationDeleteBtn').hide();
    $('#iterationName').val("");
    $('#iterationLabel').val("");
    $('#iterationFromDate').val("");
    $('#iterationToDate').val("");

    showProjects();
}


$('#iterationSaveBtn').click( function () {
    let projects = [];
    let name = $('#iterationName').val();
    let fromDate = $('#iterationFromDate').val();
    let toDate = $('#iterationToDate').val();

    if(Date.parse(fromDate) > Date.parse(toDate)) {
        warningUtils("Error", "From date must be before To date")
        return;
    }
    if(name === '') {
        warningUtils("Error", "Name must have a value")
        return;
    }

    $('#selIterationBox').children().each (function (i, option) {
        projects.push(option.value);
    });

    let iteration = {
        'name' : $('#iterationName').val(),
        'label' : $('#iterationLabel').val(),
        'fromDate' : $('#iterationFromDate').val(),
        'toDate' : $('#iterationToDate').val()
    };

    let body = {
        'project_ids': projects,
        'iteration': iteration
    }

    let url = '../api/project/historicdates';
    if(httpMethod === 'PUT') url = url + '/' + currentIterationId

    $.ajax({
        url: url,
        data: JSON.stringify(body),
        type: httpMethod,
        contentType: "application/json; charset=utf-8",
        error: function(jqXHR, textStatus, errorThrown) {
            if (jqXHR.status === 409)
                warningUtils("Error", "This iteration and subject name is already in use");
            else {
                warningUtils("Error","Error in the ElasticSearch: contact to the system administrator");
                location.href = serverUrl + "/Iterations/Configuration";
            }
        },
        success: function () {
            location.href = serverUrl + "/Iterations/Configuration";
        }
    });

})

$('#iterationDeleteBtn').click( function () {
    if (confirm("Are you sure you want to delete this iteration?")) {
        var url = "/api/project/historicdates/" + currentIterationId;
        if (serverUrl) {
            url = serverUrl + url;
        }
        $.ajax({
            url: url,
            type: "DELETE",
            error: function(jqXHR, textStatus, errorThrown) {
                warningUtils("Error", "Error in the ElasticSearch: contact to the system administrator");
                location.href = serverUrl + "/Iterations/Configuration";
            },
            success: function() {
                location.href = serverUrl + "/Iterations/Configuration";
            }
        });
    }
})

function clickOnTree(e){
    httpMethod = 'PUT';
    // mark selected iteration on the list
    e.target.classList.add("active");
    $(".iteration").each(function () {
        if (e.target.id !== $(this).attr('id'))
            $(this).removeClass("active");
    });
    currentIterationId = parseInt(e.target.id);
    let iterationData = iterations.find(i => i.id === currentIterationId);

    $('#iterationInfo').show();
    $('#iterationDeleteBtn').show();
    $('#iterationName').val(iterationData.name);
    $('#iterationLabel').val(iterationData.label);
    $('#iterationFromDate').val(iterationData.from_date);
    $('#iterationToDate').val(iterationData.to_date);

    if (projects.length > 0) {
        showProjects();
        iterationData.project_ids.forEach(function (project_id) {
            // value use metric id (because we have metric in DB)
            $('#avIterationBox').find("option[value='" + project_id + "']").appendTo('#selIterationBox');
        });
    }
}

function moveItemsLeft() {
    $('#selIterationBox').find(':selected').appendTo('#avIterationBox');
};

function moveAllItemsLeft() {
    $('#selIterationBox').children().appendTo('#avIterationBox');
};

function moveItemsRight() {
    $('#avIterationBox').find(':selected').appendTo('#selIterationBox');
};

function moveAllItemsRight() {
    $('#avIterationBox').children().appendTo('#selIterationBox');
};

function showProjects () {
    $('#avIterationBox').empty();
    $('#selIterationBox').empty();
    projects.forEach(function (project) {
        $('#avIterationBox').append($('<option>', {
            value: project.id,
            text: project.name
        }));
    });
}

window.onload = function() {
    getProjects()
    buildIterationList();
};