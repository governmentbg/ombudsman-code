<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int    $Omb_id
 * @property int    $Ar_id
 * @property int    $created_at
 * @property int    $deleted_at
 * @property int    $updated_at
 * @property Date   $Omb_date_from
 * @property Date   $Omb_date_to
 * @property string $Omb_name
 * @property string $Omb_photo
 */
class MOmbudsman extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_ombudsman';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Omb_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Ar_id', 'created_at', 'deleted_at', 'Omb_date_from', 'Omb_date_to', 'Omb_name', 'Omb_photo', 'updated_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Omb_id' => 'int', 'Ar_id' => 'int', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'Omb_date_from' => 'date', 'Omb_date_to' => 'date', 'Omb_name' => 'string', 'Omb_photo' => 'string', 'updated_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'deleted_at', 'Omb_date_from', 'Omb_date_to', 'updated_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });


        self::created(function ($article) {

            MOmbudsmanLng::create([
                'Omb_id' => $article->Omb_id,
                'S_Lng_id' => 1,
                'OmbL_title' => $article->Omb_name,
            ]);
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
    public function eq_lng()
    {
        return $this->hasMany(MOmbudsmanLng::class, 'Omb_id');
    }

    public function eq_article()
    {
        return $this->belongsTo(MArticle::class, 'Ar_id');
    }
}
