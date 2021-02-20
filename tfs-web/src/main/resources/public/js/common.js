
util = {
    ajax: function (param) {
        //showMask();

        var sf = param.success;
        if($.isFunction(sf)) {
            param.success = function(data) {
                /*if(data.code === -1 || (typeof(data) === 'string' && data.indexOf("4rfvf67687ufsaertr32") >= 0)) {
                    window.location.href = "/login.html";
                    return;
                }*/
                sf.call(document, data);
                //hideMask();
            }
        }

        var cf = param.complete;
        if($.isFunction(cf)) {
            param.complete = function() {
                cf.call(document);
                //hideMask();
            }
        }

        var ef = param.error;
        param.error = function() {
            if($.isFunction(ef)) {
                ef.call(document)
            } else {
                $.alert("网络异常");
            }
            //hideMask();
        };

        $.ajax(param);
    },
    popConfirm: function(title, content, okBtnName, cancelBtnName, okAction) {
        $.confirm({
            title: title,
            content: content,
            buttons: {
                ok: {
                    text: okBtnName,
                    btnClass: 'btn-primary',
                    action: okAction
                },
                cancel: {
                    text: cancelBtnName,
                    btnClass: 'btn-primary'
                }
            }
        });
    }
};

function showMask() {
    $('.main-container .content').loadingOverlay();
}

function hideMask() {
    $('.main-container .content').loadingOverlay('remove');
}