<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class ChangeStrMFilesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_files', function (Blueprint $table) {
            $table->integer('Mv_id')->comment('Event ID')->after('ArL_id')->nullable()->unsigned();

            $table->integer('Mn_id')->comment('News ID')->after('Mv_id')->nullable()->unsigned();


            $table->integer('ArL_id')->comment('Article Lng ID')->unsigned()->nullable()->change();




            $table->foreign('Mn_id')->references('Mn_id')->on('m_news');
            $table->foreign('Mv_id')->references('Mv_id')->on('m_event');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_files', function (Blueprint $table) {
            //
        });
    }
}
