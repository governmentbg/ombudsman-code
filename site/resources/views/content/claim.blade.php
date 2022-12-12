@extends('layouts.default')
@section('title', $data->ArL_title . ' - ' . trans('common.title'))
@section('site-description', $data->Ar_name)
@section('site-keywords', trans('page-keywords'))
{{-- @section('site-canonical', $canonical) --}}
{{-- @section('page-hreflang', $canonical) --}}
@section('article-headline', $data->ArL_title)
{{-- {{-- @section('article-date', $publishDate) --}}
@section('article-modification-date', $data->created_at)
@section('article-description', trans('page-description'))
@section('article-tag', '')


@section('content')
    @include('layouts.breadcrumb')

    {{-- {{ futureDate('05/06/2021') }} --}}
    {{-- {{ diffDates('05/07/2022') }} --}}

    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ $data->ArL_title }}</h1>
        </div>
    </div>

    <div class=" mb-5 m-article">
        {{-- <h1 class="">{{ $data->ArL_title }}</h1> --}}
        <div class="row m-info">
            @if (Request::segment(1) == 'lr')
                <div class="col-sm-12  col-xs-12  col-lg-12 col-md-12 pl-0">
                @else
                    <div class="col-sm-12  col-xs-12  col-lg-8 col-md-8 pl-0">
            @endif



            {{-- {{dd($res["test"])}} --}}

            {{-- @foreach ($res['test'] as $fq)
     
          {{dd($fq)}}
      
     @endforeach --}}
            {{-- {{ $req->name }} --}}
            {{-- <i class="cis-info text-success"></i>222 --}}
            <ul class="list-unstyled">
                @foreach ($error as $err)
                    <li> <i class="cis-x text-danger"> </i> {{ $err }}</li>
                @endforeach
                @foreach ($info as $inf)
                    <li> <i class="cis-ban text-success"></i> </i> {{ $inf }}</li>
                @endforeach
            </ul>

            <div class="m-card p-3">
                <div class="m-form form">
                    <form method="POST" action="/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 49) }}"
                        enctype="multipart/form-data">
                        @csrf
                        <div class="row">
                            {{-- {{ $req['claimKey'] }} --}}
                            <input type="hidden" name="claimKey" value="{{ $req['claimKey'] }}" />

                            <div class="col-12">
                                <div class="custom-control custom-radio custom-control-inline">
                                    <input type="radio" name="persType" value="4" id="persType4" @php
                                        if ($req['persType'] == 4) {
                                            echo "checked=\"checked\"";
                                        }
                                    @endphp
                                        onclick="show2();" class="custom-control-input">
                                    <label class="custom-control-label" for="persType4">
                                        {{ trans('common.claim.person') }}
                                    </label>
                                </div>
                                <div class="custom-control custom-radio custom-control-inline">
                                    <input type="radio" name="persType" value="3" id="persType3" @php
                                        if ($req['persType'] == 3) {
                                            echo "checked=\"checked\"";
                                        }
                                    @endphp
                                        onclick="show1(); " class="custom-control-input">
                                    <label class="custom-control-label" for="persType3">
                                        {{ trans('common.claim.company') }}
                                    </label>
                                </div>



                                {{-- <div class="float-right">
                                        <a href="/bg/p/poday-signal">
                                            <i class="cis-external-link"></i>
                                            Ако си дете подай сигнал тук
                                        </a>
                                    </div> --}}
                            </div>
                        </div>
                        <div class="row" id="div1_1">

                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="name">{{ trans('common.claim.name') }}:</label>
                                    <input type="" class="form-control" name="name" placeholder=""  title="{{ trans('common.claim.name') }}" aria-label="{{ trans('common.claim.name') }}"
                                        value="{{ $req['name'] }}">
                                </div>
                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="egn">{{ trans('common.claim.egn') }}:</label>
                                    <input type="" class="form-control" name="egn" placeholder=""  title="{{ trans('common.claim.egn') }}" aria-label="{{ trans('common.claim.egn') }}"
                                        value="{{ $req['egn'] }}">
                                </div>
                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="lnch">{{ trans('common.claim.lnch') }}:</label>
                                    <input type="" class="form-control" name="lnch" placeholder=""  title="{{ trans('common.claim.lnch') }}" aria-label="{{ trans('common.claim.lnch') }}"
                                        value="{{ $req['lnch'] }}">
                                </div>
                            </div>


                        </div>
                        <div class="row" id="div2_1">

                            <div class="col-sm-12 col-xs-12 col-lg-8 col-md-8">
                                <div class="form-group">
                                    <label for="name1">{{ trans('common.claim.company_name') }}:</label>
                                    <input type="" class="form-control" name="name1" placeholder=""  title="{{ trans('common.claim.company_name') }}" aria-label="{{ trans('common.claim.company_name') }}"
                                        value="{{ $req['name1'] }}">
                                </div>

                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="eik">{{ trans('common.claim.eik') }}:</label>
                                    <input type="" class="form-control" name="eik" placeholder=""  title="{{ trans('common.claim.eik') }}" aria-label="{{ trans('common.claim.eik') }}"
                                        value="{{ $req['eik'] }}">
                                </div>
                            </div>



                        </div>
                        <div class="row" id="div1_2">

                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="age">{{ trans('common.claim.age') }}:</label>
                                    <input type="" class="form-control" name="age" placeholder=""  title="{{ trans('common.claim.age') }}" aria-label="{{ trans('common.claim.age') }}"
                                        value="{{ $req['age'] }}">
                                </div>
                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">

                                <div class="form-group">
                                    <label for="gender">{{ trans('common.claim.gender') }}:</label>
                                    <select class="form-control js-example-basic-single"  title="{{ trans('common.claim.gender') }}" aria-label="{{ trans('common.claim.gender') }}"name="gender">


                                        @foreach ($res['gender'] as $ar)
                                            @if ($ar['code'] == $req['gender'])
                                                <option selected value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                            @else
                                                <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                            @endif

                                            {{-- <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option> --}}
                                        @endforeach


                                    </select>
                                </div>
                            </div>
                            {{-- {{ dd($res['vid']) }} --}}
                            <hr>
                            {{-- {{ dd($res['gender']) }} --}}







                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">

                                <div class="form-group">
                                    <label for="citizen">{{ trans('common.claim.citizen') }}:</label>
                                    <select class="form-control js-example-basic-single m-select2" name="citizen"  title="{{ trans('common.claim.citizen') }}" aria-label="{{ trans('common.claim.citizen') }}">
                                        <option value="37">{{ trans('common.claim.bg') }}</option>

                                        @foreach ($res['citizen'] as $ar)
                                            @if ($ar['code'] == $req['citizen'])
                                                <option selected value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                </option>
                                            @else
                                                <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                            @endif

                                            {{-- <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option> --}}
                                        @endforeach




                                    </select>
                                </div>




                            </div>


                        </div>
                        <div class="row">

                            <div class="col-sm-12 col-xs-12 col-lg-8 col-md-8">
                                <div class="form-group">
                                    <label for="city">{{ trans('common.claim.city') }}:</label>
                                    {{-- <input type="" class="form-control" name="city"  > --}}
                                    <select class="form-control js-example-basic-single m-select2" name="city"  title="{{ trans('common.claim.city') }}" aria-label="{{ trans('common.claim.city') }}">
                                        <option value="">{{ trans('common.reg.pickList') }}</option>


                                        @foreach ($res['city'] as $ar)
                                            @if ($ar['code'] == $req['city'])
                                                <option selected value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                    ({{ $ar['dopInfo'] }})
                                                </option>
                                            @else
                                                <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                    ({{ $ar['dopInfo'] }})</option>
                                            @endif

                                            {{-- <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option> --}}
                                        @endforeach



                                    </select>
                                </div>
                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="zip">{{ trans('common.claim.zip') }}:</label>
                                    <input type="" class="form-control" name="zip" placeholder=""  title="{{ trans('common.claim.zip') }}" aria-label="{{ trans('common.claim.zip') }}"
                                        value="{{ $req['zip'] }}">
                                </div>

                            </div>


                        </div>


                        <div class="row">


                            <div class="col-sm-12 col-xs-12 col-lg-12 col-md-12">
                                <div class="form-group">
                                    <label for="address">{{ trans('common.claim.address') }}:</label>
                                    <input type="" class="form-control" name="address" placeholder=""  title="{{ trans('common.claim.address') }}" aria-label="{{ trans('common.claim.address') }}"
                                        value="{{ $req['address'] }}">
                                </div>
                            </div>

                        </div>


                        <div class="row">

                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="phone">{{ trans('common.claim.phone') }}:</label>
                                    <input type="" class="form-control" name="phone" placeholder=""  title="{{ trans('common.claim.phone') }}" aria-label="{{ trans('common.claim.phone') }}"
                                        value="{{ $req['phone'] }}">
                                </div>

                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="email">E-mail:</label>
                                    <input type="" class="form-control" name="email" placeholder=""  title="E-mail" aria-label="E-mail"
                                        value="{{ $req['email'] }}">
                                </div>
                            </div>
                            <div class="form-check col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4  pt-3">

                                <div class="custom-control custom-checkbox custom-control-inline">
                                    <input type="checkbox" name="protect" id="protect" class="  custom-control-input"  title="{{ trans('common.claim.protect') }}" aria-label="{{ trans('common.claim.protect') }}"
                                        value="1" @php
                                            if ($req['protect'] == 1) {
                                                echo "checked=\"checked\"";
                                            }
                                        @endphp>
                                    <label class="custom-control-label"
                                        for="protect">{{ trans('common.claim.protect') }}</label>
                                </div>

                            </div>


                        </div>

                        <div class="row">



                            <div class="col-sm-12 col-xs-12 col-lg-8 col-md-8">
                                <div class="form-group">
                                    <label for="defendant">{{ trans('common.claim.defendant') }}:</label>
                                    <input type="text" class="form-control" name="defendant"
                                        value="{{ $req['defendant'] }}"  title="{{ trans('common.claim.defendant') }}" aria-label="{{ trans('common.claim.defendant') }}">
                                </div>

                            </div>

                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                <div class="form-group">
                                    <label for="date">{{ trans('common.claim.date') }}:</label>

                                    <input type="text" id="datepicker1"
                                        placeholder="{{ trans('common.claim.date_format') }}" class="form-control"  title="{{ trans('common.claim.date') }}" aria-label="{{ trans('common.claim.date') }}"
                                        name="date" value="{{ $req['date'] }}">

                                </div>

                            </div>



                        </div>

                        <div class="row">

                            <div class="col-sm-12 col-xs-12 col-lg-8 col-md-8">
                                <div class="form-group">
                                    <label for="rights">{{ trans('common.claim.rights') }}:</label>
                                    <select class="form-control js-example-basic-single m-select2" name="rights"  title="{{ trans('common.claim.rights') }}" aria-label="{{ trans('common.claim.rights') }}">
                                        <option value="">{{ trans('common.reg.pickList') }} </option>


                                        @foreach ($res['rights'] as $ar)
                                            @if ($ar['code'] == $req['rights'])
                                                <option selected value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                </option>
                                            @else
                                                <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                            @endif

                                            {{-- <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option> --}}
                                        @endforeach

                                    </select>
                                </div>
                            </div>
                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">

                                <div class="form-group">
                                    <label for="vid">{{ trans('common.claim.vid') }}:</label>
                                    <select class="form-control js-example-basic-single m-select2" name="vid"  title="{{ trans('common.claim.vid') }}" aria-label="{{ trans('common.claim.vid') }}">

                                        <option value=""></option>
                                        {{-- {{ ddd($res['vid']) }} --}}

                                        @if ($res['vid'])
                                            @foreach ($res['vid'] as $ar)
                                                @if ($ar['code'] == $req['vid'])
                                                    <option selected value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                    </option>
                                                @else
                                                    <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                                @endif

                                                {{-- <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option> --}}
                                            @endforeach
                                        @endif

                                    </select>
                                </div>
                            </div>


                        </div>
                        <div class="row">

                            <div class="col-lg-12 col-xl-12 col-md-12 col-sm-12 col-xs-12">
                                <div class="form-group">
                                    <label for="descr">{{ trans('common.claim.descr') }}:</label>
                                    <textarea class="form-control ta" rows="4" id="descr"   title="{{ trans('common.claim.descr') }}" aria-label="{{ trans('common.claim.descr') }}"name="descr">{{ $req['descr'] }}</textarea>
                                </div>
                            </div>
                        </div>
                        <div class="row">
                            {{-- {{ dd($req) }} --}}
                            <div class="col-lg-12 col-xl-12 col-md-12 col-sm-12 col-xs-12">
                                <div class="form-group">
                                    <label for="request1">{{ trans('common.claim.request') }}:</label>
                                    <textarea class="form-control ta" rows="4" id="request1"  title="{{ trans('common.claim.request') }}" aria-label="{{ trans('common.claim.request') }}" name="request1">{{ $req['request1'] }}</textarea>
                                </div>
                            </div>
                        </div>

                        <div class="row">

                            <div class="col-12">{{ trans('common.claim.considered') }}
                                <div class="custom-control custom-radio custom-control-inline">
                                    <input type="radio" name="considered" value="1" id="considered"
                                        @php
                                            if ($req['considered'] == 1) {
                                                echo "checked=\"checked\"";
                                            }
                                        @endphp onclick="show3();" class="custom-control-input">
                                    <label class="custom-control-label"
                                        for="considered">{{ trans('common.claim.considered_yes') }}</label>
                                </div>
                                <div class="custom-control custom-radio custom-control-inline">
                                    <input type="radio" name="considered" value="0" @php
                                        if ($req['considered'] == 0) {
                                            echo "checked=\"checked\"";
                                        }
                                    @endphp
                                        id="considered2" onclick="show4();" class="custom-control-input">
                                    <label class="custom-control-label"
                                        for="considered2">{{ trans('common.claim.considered_no') }}</label>
                                </div>




                            </div>



                        </div>

                        <div class="row" id="div3_1">

                            <div class="col-sm-12 col-xs-12 col-lg-8 col-xl-8 col-md-8">
                                <input type="" class="form-control" name="consideredBy"
                                    placeholder="{{ trans('common.claim.consideredBy') }}"  title="{{ trans('common.claim.consideredBy') }}" aria-label="{{ trans('common.claim.consideredBy') }}"
                                    value="{{ $req['consideredBy'] }}">
                            </div>



                        </div>
                        <div class="row">
                            <div class="col-sm-12 col-xs-12 col-lg-8 col-xl-8 col-md-8">

                                <div class="form-group">
                                    <label for="answer">{{ trans('common.claim.answer') }}:</label>
                                    <select class="form-control js-example-basic-single m-select2" name="answer"  title="{{ trans('common.claim.answer') }}" aria-label="{{ trans('common.claim.answer') }}">

                                        <option value="">{{ trans('common.reg.pickList') }} </option>

                                        @foreach ($res['answer'] as $ar)
                                            @if ($ar['code'] == $req['answer'])
                                                <option selected value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                </option>
                                            @else
                                                <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                            @endif

                                            {{-- <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option> --}}
                                        @endforeach





                                    </select>
                                </div>



                                {{-- <input type="" class="form-control"  placeholder="Гражданство"> --}}
                            </div>
                        </div>
                        <div class="row">

                            <div class="col-sm-12 col-xs-12 col-lg-8 col-md-8 col-xl-8">
                                <div class="custom-file">
                                    <input class="custom-file-input" name="file[]" type="file" multiple  title="{{ trans('common.claim.file') }}" aria-label="{{ trans('common.claim.file') }}"
                                        onchange="javascript:updateList()" data-show-upload="true"
                                        data-show-caption="true" id="file">
                                    <label class="custom-file-label"
                                        for="file">{{ trans('common.claim.file') }}</label>

                                    {{-- <input type="text" class="form-input-data" name="fileBox[]" id="inputBox"
                                        value="{{ $req['fileBox'] }}" readonly> --}}

                                    <div id="fileBox" class="text-muted">{{ $req['fileBox'] }}


                                        {{-- {{ dd($req['fileList']) }} --}}
                                        @foreach ($req['fileList'] as $fl)
                                            {{ $fl->R_ClF_name }};
                                        @endforeach
                                        {{-- {{ $req['fileList'] }} --}}
                                    </div>



                                    <script>
                                        // let text = "";
                                        // const fileInput = document.getElementById('file');
                                        // fileInput.onchange = () => {
                                        //     const selectedFile = fileInput.files;

                                        //     // document.getElementById('inputBox').value = selectedFile.name;


                                        //     selectedFile.forEach(myFunction);

                                        //     document.getElementById("inputBox").value = text;

                                        //     function myFunction(selectedFile, index) {
                                        //         text += index + ": " + selectedFile.name + ";";
                                        //     }
                                        // }

                                        updateList = function() {
                                            var input = document.getElementById('file');
                                            var output = document.getElementById('fileBox');

                                            output.innerHTML = '';
                                            for (var i = 0; i < input.files.length; ++i) {
                                                output.innerHTML += '' + input.files.item(i).name + ';';
                                            }
                                            output.innerHTML += '';
                                        }
                                    </script>
                                </div>


                                {{-- <div class="form-group">
                                    <label for="file ">{{ trans('common.claim.file') }}:</label>
                                    <div class="">
                                        <span class="btn btn-default btn-file">
                                            <input id="file" name="file" type="file" class="file" multiple
                                                data-show-upload="true" data-show-caption="true" "
    >
                                                </span>
                                               
                                            </div>
                                        </div> --}}

                            </div>

                        </div>



                        <div class="row">
                            <div class="col-12 text-right">
                                <button type="submit" title="{{ trans('common.claim.send') }}" class="btn m-btn">{{ trans('common.claim.send') }}</button>
                            </div>


                        </div>


                    </form>

                </div>

            </div>


            <div class="">

            </div>


        </div>

        @if (Request::segment(1) != 'lr')
            <div class="col-sm-12  col-xs-12 col-lg-4 col-xl-4 col-md-4  p-0 m-cp-card">

                <div id="accordion">

                    @php
                        $i = 0;
                        
                    @endphp
                    @foreach (\App\Http\Controllers\MFaqController::fqList(App::getLocale(), 8, 2) as $fq)
                        @php
                            $i++;
                            
                        @endphp
                        <div class="card">
                            <div class="card-header" id="heading{{ $fq->Fq_id }}">
                                <h5 class="mb-0">
                                    <button class="btn btn-link" data-toggle="collapse"
                                        data-target="#collapse{{ $fq->Fq_id }}" aria-expanded="true"
                                        aria-controls="collapse{{ $fq->Fq_id }}">
                                        {{ $fq->FqL_title }}
                                    </button>
                                </h5>
                            </div>


                            <div id="collapse{{ $fq->Fq_id }}"
                                class="collapse  @php if($i==1) { echo 'show';} @endphp"
                                aria-labelledby="heading{{ $fq->Fq_id }}" data-parent="#accordion">
                                <div class="card-body">


                                    {!! $fq->FqL_body !!}


                                </div>
                            </div>
                        </div>
                        {{--  --}}
                    @endforeach


                </div>





            </div>
        @endif





    </div>
    </div>





    <script>
        // window.onload = show2()
        // window.onload = show4()

        function show1() {
            document.getElementById('div1_1').style.display = 'none';
            document.getElementById('div1_2').style.display = 'none';
            document.getElementById('div2_1').style.display = '';
        }

        function show2() {
            document.getElementById('div1_1').style.display = '';
            document.getElementById('div1_2').style.display = '';
            document.getElementById('div2_1').style.display = 'none';
        }

        function show3() {
            document.getElementById('div3_1').style.display = '';
        }

        function show4() {
            document.getElementById('div3_1').style.display = 'none';
        }


        $(document).ready(function() {
            jQuery('select[name="rights"]').on('change', function() {
                var rightsID = jQuery(this).val();
                var lngId = {{ i18n(App::getLocale()) }};
                if (rightsID) {
                    jQuery.ajax({
                        url: '/type-claim/' + lngId + '/' + rightsID,
                        type: "GET",
                        dataType: "json",
                        success: function(data) {
                            // console.log(rightsID);
                            // console.log(data);
                            jQuery('select[name="vid"]').empty();
                            jQuery.each(data, function(key, value) {
                                // console.log(value);
                                // console.log(value.code);
                                // console.log(value.tekst);
                                $('select[name="vid"]').append('<option value="' +
                                    value.code +
                                    '">' + value.tekst + '</option>');
                            });
                        }
                    });
                } else {
                    $('select[name="vid"]').empty();
                }
            });
        });
    </script>



    @if ($req['persType'] == 4)
        <script>
            window.onload = show2()
        </script>
    @else
        <script>
            window.onload = show1()
        </script>
    @endif

    @if ($req['considered'] == 1)
        <script>
            window.onload = show3()
        </script>
    @else
        <script>
            window.onload = show4()
        </script>
    @endif

@stop
