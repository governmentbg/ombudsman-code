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
    <div class="row">
        <div class="col ">
            <h1 class="t-h1">{{ $data->ArL_title }}</h1>
        </div>
    </div>


    <div class=" mb-5 m-article">


        <div class="row m-info">
            <div class="col-sm-12  col-12  col-lg-8 col-md-8 pl-0">



                {{-- {{dd($res["test"])}} --}}

                {{-- @foreach ($res['test'] as $fq)
     
          {{dd($fq)}}

     @endforeach --}}

                <ul class="list-unstyled text-white">
                    @foreach ($error as $err)
                        <li> <i class="cis-x text-danger"> </i> {{ $err }}</li>
                    @endforeach

                    @foreach ($info as $inf)
                        <li> <i class="cis-ban text-success"></i> </i> {{ $inf }}</li>
                    @endforeach
                </ul>
                <div class="m-card p-3">
                    <div class="m-form form">
                        <form method="POST"
                            action="/prava-na-deteto/{{ App::getLocale() }}/p/{{ getPathById(App::getLocale(), 39) }}"
                            enctype="multipart/form-data">
                            @csrf
                            <input type="hidden" name="claimKey" value="{{ $req['claimKey'] }}" />
                            <div class="row">


                                <div class="col-12">
                                    <div class="custom-control custom-radio custom-control-inline">
                                        <input type="radio" name="persType" value="1" id="persType4"
                                            checked="checked" class="custom-control-input">
                                        <label class="custom-control-label"
                                            for="persType4">{{ trans('common.claim.boy') }}</label>
                                    </div>
                                    <div class="custom-control custom-radio custom-control-inline">
                                        <input type="radio" name="persType" value="2" id="persType3"
                                            class="custom-control-input">
                                        <label class="custom-control-label"
                                            for="persType3">{{ trans('common.claim.girl') }}</label>
                                    </div>

                                    <input type="hidden" name="cr" value="1" />





                                </div>
                            </div>

                            <div class="row" id="div1_1">

                                <div class="col-sm-12 col-xs-12 col-lg-8 col-xl-8 col-md-8">
                                    <div class="form-group">
                                        <label for="name">{{ trans('common.claim.yourName') }}:</label>
                                        <input type="" class="form-control" name="name"
                                            value="{{ $req['name'] }}" placeholder=""  title="{{ trans('common.claim.yourName') }}" aria-label="{{ trans('common.claim.yourName') }}">
                                    </div>
                                </div>
                                <div class="col-sm-12 col-12 col-lg-4 col-xl-4 col-md-4">
                                    <div class="form-group">
                                        <label for="age">{{ trans('common.claim.yourAge') }}:</label>
                                        <input type="" class="form-control" name="age"
                                            value="{{ $req['age'] }}" placeholder=""  title="{{ trans('common.claim.yourAge') }}" aria-label="{{ trans('common.claim.yourAge') }}">
                                    </div>
                                </div>



                            </div>


                            <div class="row">

                                <div class="col-sm-12 col-xs-12 col-lg-8 col-xl-8 col-md-8">
                                    <div class="form-group">
                                        <label for="city">{{ trans('common.claim.yourCity') }}:</label>

                                        <select class="form-control js-example-basic-single m-select2" name="city"  title="{{ trans('common.claim.yourCity') }}" aria-label="{{ trans('common.claim.yourCity') }}">

                                            <option value="">{{ trans('common.reg.pickList') }}
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
                                        <input type="" class="form-control" name="zip"
                                            value="{{ $req['zip'] }}" placeholder=""  title="{{ trans('common.claim.zip') }}" aria-label="{{ trans('common.claim.zip') }}">
                                    </div>

                                </div>


                            </div>


                            <div class="row">


                                <div class="col-sm-12 col-xs-12 col-lg-12 col-md-12 col-xl-12">
                                    <div class="form-group">
                                        <label for="address">{{ trans('common.claim.address') }}:</label>
                                        <input type="" class="form-control" name="address"
                                            value="{{ $req['address'] }}" placeholder=""  title="{{ trans('common.claim.address') }}" aria-label="{{ trans('common.claim.address') }}">
                                    </div>
                                </div>

                            </div>


                            <div class=" row">

                                <div class="col-sm-12 col-12 col-lg-6 col-xl-6 col-md-6">
                                    <div class="form-group">
                                        <label for="phone">{{ trans('common.claim.phone') }}:</label>
                                        <input type="" class="form-control" name="phone"
                                            value="{{ $req['phone'] }}" placeholder=""  title="{{ trans('common.claim.phone') }}" aria-label="{{ trans('common.claim.phone') }}">
                                    </div>

                                </div>
                                <div class="col-sm-12 col-12 col-lg-6 col-xl-6 col-md-6">
                                    <div class="form-group">
                                        <label for="email">E-mail:</label>
                                        <input type="" class="form-control" name="email" placeholder=""  title="E-mail" aria-label="E-mail"
                                            value="{{ $req['email'] }}">
                                    </div>
                                </div>



                            </div>


                            <div class="row">

                                <div class="col-lg-12 col-xl-12 col-md-12 col-sm-12 col-xs-12">
                                    <div class="form-group">
                                        <label for="descr">{{ trans('common.claim.problem') }}:</label>
                                        <textarea class="form-control tea" rows="6"   title="{{ trans('common.claim.problem') }}" aria-label="{{ trans('common.claim.problem') }}"id="descr" name="descr">{{ $req['descr'] }}</textarea>
                                    </div>
                                </div>
                            </div>
                            {{-- <div class="row">

                                <div class="col-lg-12 col-xl-12 col-md-12 col-sm-12 col-xs-12">
                                    <div class="form-group">
                                        <label for="request1">{{ trans('common.claim.help') }}:</label>
                                        <textarea class="form-control ta" rows="4" id="request1" name="request1">{{ $req['request1'] }}</textarea>
                                    </div>
                                </div>
                            </div> --}}




                            {{-- <div class="row">
                                <div class="col-sm-12 col-xs-12  col-lg-8 col-xl-8 col-md-8">

                                    <div class="form-group">
                                        <label for="answer">{{ trans('common.claim.answerType') }}:</label>
                                        <select class="form-control js-example-basic-single m-select2" name="answer">
                                            @foreach ($res['answer'] as $ar)
                                                <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}</option>
                                            @endforeach

                                        </select>
                                    </div>




                                </div>
                            </div> --}}
                            {{-- <div class="row">

                                <div class="col-sm-12 col-xs-12  col-lg-8 col-xl-8 col-md-8"> --}}

                            {{-- <div class="custom-file">
                                        <input class="custom-file-input" name="file" type="file" class="file"
                                            multiple data-show-upload="true" data-show-caption="true" id="customFile">
                                        <label class="custom-file-label"
                                            for="customFile">{{ trans('common.claim.file') }}</label>
                                    </div> --}}

                            {{-- <div class="form-group">
                                        <label for="comment ">Прикачане на файл:</label>
                                        <div class="">
                                            <span class="btn btn-default btn-file">
                                                <input id="input-2" name="input2[]" type="file" class="file "
                                                    multiple data-show-upload="true" data-show-caption="true">
                                            </span>
                                        </div>
                                    </div> --}}

                            {{-- </div>

                            </div> --}}

                            <div class="row">
                                <div class="col-12 text-center">
                                    <button type="submit" title="{{ trans('common.claim.signal') }}"
                                        class="btn m-btn m-cr-btn ">{{ trans('common.claim.signal') }}</button>
                                </div>


                            </div>


                        </form>

                    </div>

                </div>


                <div class="">

                </div>


            </div>






        </div>
    </div>

    <script>
        window.onload = show2()
        window.onload = show4()

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
    </script>

@stop
