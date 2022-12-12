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

    <div class="row">
        <div class="col">
            <div class="mb-5 m-article pt-0">


                {{-- {{ dd($res) }} --}}
                {{-- {{ dd($res['results']) }} --}}
                <p>
                    <a class="btn m-collapse" data-toggle="collapse" href="#searchForm" role="button" aria-expanded="false"
                        aria-controls="multiCollapseExample1"><i class="cis-search"></i>
                        {{ trans('common.reg.search_form') }} </a>

                </p>
                <div class="row mb-3">
                    <div class="col">
                        <div class="collapse multi-collapse" id="searchForm">
                            <div class="m-card card1 card-body ">
                                <div class="m-form form">
                                    <form>
                                        <div class="row">
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="rnDoc">{{ trans('common.reg.recNum') }}:</label>
                                                    <input type="" class="form-control" name="rnDoc"
                                                        placeholder="">
                                                </div>
                                            </div>

                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="docDateOt">{{ trans('common.reg.fromDate') }}</label>

                                                    <input type="text" id="datepicker" placeholder=""
                                                        class="form-control" name="docDateOt">

                                                </div>

                                            </div>
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="docDateOt">{{ trans('common.reg.toDate') }}</label>

                                                    <input type="text" id="datepicker1" placeholder=""
                                                        class="form-control" name="docDateDo">

                                                </div>

                                            </div>



                                        </div>
                                        <div class="row">
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="predmet">{{ trans('common.reg.inspSubject') }}:</label>
                                                    <input type="" class="form-control" name="predmet"
                                                        placeholder="">
                                                </div>
                                            </div>

                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="narPrava">{{ trans('common.reg.viRights') }}:</label>
                                                    <select class="form-control js-example-basic-single m-select2"
                                                        name="narPrava">
                                                        <option value="">{{ trans('common.reg.pickList') }}
                                                        </option>
                                                        @foreach ($res->narPrava as $ar)
                                                            <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                            </option>
                                                        @endforeach

                                                    </select>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="codeOrgan">{{ trans('common.reg.inspUnit') }}:</label>
                                                    <select class="form-control js-example-basic-single m-select2"
                                                        name="codeOrgan">
                                                        <option value="">{{ trans('common.reg.pickList') }}
                                                        </option>
                                                        @foreach ($res->codeOrgan as $ar)
                                                            <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                            </option>
                                                        @endforeach

                                                    </select>
                                                </div>
                                            </div>
                                        </div>
                                        <div class="row">
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="konstat">{{ trans('common.reg.findings') }}:</label>
                                                    <input type="" class="form-control" name="konstat"
                                                        placeholder="">
                                                </div>
                                            </div>

                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="prepor">{{ trans('common.reg.recom') }}:</label>
                                                    <select class="form-control js-example-basic-single m-select2"
                                                        name="prepor">
                                                        <option value="">{{ trans('common.reg.pickList') }}
                                                        </option>
                                                        @foreach ($res->prepor as $ar)
                                                            <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                            </option>
                                                        @endforeach

                                                    </select>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="vidResult">{{ trans('common.reg.result') }}:</label>
                                                    <select class="form-control js-example-basic-single m-select2"
                                                        name="vidResult">
                                                        <option value="">{{ trans('common.reg.pickList') }}
                                                        </option>
                                                        @foreach ($res->vidResult as $ar)
                                                            <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                            </option>
                                                        @endforeach

                                                    </select>
                                                </div>
                                            </div>
                                        </div>






                                        <div class="row">
                                            <div class="col-sm-12 col-xs-12 col-lg-4 col-xl-4 col-md-4">
                                                <div class="form-group">
                                                    <label for="sast">{{ trans('common.reg.condition') }}:</label>
                                                    <select class="form-control js-example-basic-single m-select2"
                                                        name="sast">
                                                        <option value="">{{ trans('common.reg.pickList') }}
                                                        </option>
                                                        @foreach ($res->sast as $ar)
                                                            <option value="{{ $ar['code'] }}">{{ $ar['tekst'] }}
                                                            </option>
                                                        @endforeach

                                                    </select>
                                                </div>
                                            </div>
                                            <div class="col-sm-12 col-xs-12 col-lg-8 col-md-8 text-right">
                                                <button type="submit"
                                                    class="btn m-btn ">{{ trans('common.search_label') }}</button>
                                            </div>


                                        </div>


                                    </form>

                                </div>

                            </div>
                        </div>
                    </div>

                </div>

                <table class="table table-responsive table-striped table-hover">
                    <thead>
                        <tr>
                            {{-- <th scope="col">Вх. №</th> --}}
                            <th scope="col">{{ trans('common.reg.numDate') }} </th>
                            <th scope="col">{{ trans('common.reg.inspSubject') }}</th>
                            <th scope="col">{{ trans('common.reg.viRights') }}</th>
                            <th scope="col">{{ trans('common.reg.inspUnit') }}</th>
                            <th scope="col">{{ trans('common.reg.capacityUnit') }}</th>
                            <th scope="col">{{ trans('common.reg.accomPersons') }}</th>
                            <th scope="col">{{ trans('common.reg.findings') }}</th>
                            <th scope="col">{{ trans('common.reg.recom') }}</th>
                            <th scope="col">{{ trans('common.reg.result') }}</th>
                            <th scope="col">{{ trans('common.reg.condition') }}</th>
                        </tr>
                    </thead>
                    <tbody>
                        @foreach ($res['results'] as $res)
                            {{-- {{dd($res["doc_id"])}} --}}
                            <tr>

                                {{-- <td>{{$res["doc_id"]}}</td> --}}
                                <td><span class="badge badge-info">{{ $res['rn_doc'] }}</span>
                                    {{ Carbon\Carbon::createFromDate($res['doc_date'])->format('d/m/Y') }}
                                </td>
                                <td>{{ $res['predmet'] }}</td>
                                <td>{{ $res['nar_prava_text'] }}</td>
                                <td>{{ $res['code_organ_text'] }}</td>
                                <td>{{ $res['capacity'] }}</td>
                                <td>{{ $res['nas_lica'] }}</td>
                                <td>{{ $res['konstat'] }}</td>
                                <td>{{ $res['prepor_text'] }}</td>
                                <td>{{ $res['vid_result_text'] }}</td>
                                <td>{{ $res['sast_text'] }}</td>


                            </tr>
                        @endforeach

                    </tbody>
                </table>

            </div>
        </div>
    </div>





@stop
